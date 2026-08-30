package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.EventFullDto;
import ru.practicum.main.dto.EventShortDto;
import ru.practicum.main.dto.NewEventDto;
import ru.practicum.main.dto.UpdateEventRequest;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        log.debug("Создание события пользователем userId={}", userId);

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));

        if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа");
        }

        Event event = EventMapper.toEntity(request, initiator, category);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);
        log.debug("Событие создано с id={}", saved.getId());

        return EventMapper.toFullDto(saved, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.debug("Получение событий пользователя userId={}", userId);
        ensureUserExists(userId);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        return toShortDtos(events);
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.debug("Получение события userId={}, eventId={}", userId, eventId);

        Event event = findEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventRequest request) {
        log.debug("Обновление события userId={}, eventId={}", userId, eventId);

        Event event = findEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Нельзя изменить опубликованное событие");
        }

        if (request.getEventDate() != null
                && request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть не раньше чем через 2 часа");
        }

        updateEventFields(event, request);

        if (UserStateAction.SEND_TO_REVIEW.name().equals(request.getStateAction())) {
            event.setState(EventState.PENDING);
        } else if (UserStateAction.CANCEL_REVIEW.name().equals(request.getStateAction())) {
            event.setState(EventState.CANCELED);
        }

        Event updated = eventRepository.save(event);
        log.debug("Событие обновлено id={}", updated.getId());

        return toFullDto(updated);
    }

    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        log.debug("Получение событий для администратора");

        List<EventState> eventStates = states == null
                ? null
                : states.stream().map(EventState::valueOf).toList();

        LocalDateTime start = rangeStart == null
                ? LocalDateTime.of(1970, 1, 1, 0, 0)
                : rangeStart;

        LocalDateTime end = rangeEnd == null
                ? LocalDateTime.of(9999, 12, 31, 23, 59)
                : rangeEnd;

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findAllByAdmin(
                users,
                eventStates,
                categories,
                start,
                end,
                pageable
        ).getContent();

        return toFullDtos(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventRequest request) {
        log.debug("Обновление события администратором eventId={}", eventId);

        Event event = findEvent(eventId);

        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Дата события не может быть в прошлом");
        }

        updateEventFields(event, request);

        if (AdminStateAction.PUBLISH_EVENT.name().equals(request.getStateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Событие можно опубликовать только в статусе PENDING");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (AdminStateAction.REJECT_EVENT.name().equals(request.getStateAction())) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Нельзя отклонить опубликованное событие");
            }
            event.setState(EventState.CANCELED);
        }

        Event updated = eventRepository.save(event);
        log.debug("Событие обновлено админом id={}", updated.getId());

        return toFullDto(updated);
    }

    @Override
    public List<EventShortDto> getEventsForPublic(String text, List<Long> categories, Boolean paid,
                                                  String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                                  String sort, int from, int size) {
        log.debug("Получение событий для публичного доступа: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        LocalDateTime start = parseDate(rangeStart, true);
        LocalDateTime end = parseDate(rangeEnd, false);

        if (categories != null && !categories.isEmpty()) {
            for (Long catId : categories) {
                if (catId == null || catId <= 0) {
                    throw new BadRequestException("Invalid category id: " + catId);
                }
            }
        }

        Sort sorting = Sort.unsorted();
        if (EventSort.EVENT_DATE.name().equalsIgnoreCase(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "eventDate");
        }

        Pageable pageable = PageRequest.of(from / size, size, sorting);

        List<Event> events = eventRepository.findAllByPublic(
                categories == null || categories.isEmpty() ? null : categories,
                paid,
                start,
                end,
                Boolean.TRUE.equals(onlyAvailable),
                pageable
        ).getContent();

        log.debug("Найдено событий: {}", events.size());

        return toShortDtos(events);
    }

    @Override
    public EventFullDto getEventForPublic(Long eventId, HttpServletRequest request) {
        log.debug("Получение события для публичного доступа eventId={}", eventId);

        Event event = findEvent(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id " + eventId + " не опубликовано");
        }

        // Статистика сохраняется в контроллере — дублирование убрано

        return toFullDto(event);
    }

    private void updateEventFields(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(findCategory(request.getCategory()));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLocation(new ru.practicum.main.model.Location(
                    request.getLocation().getLat(),
                    request.getLocation().getLon()
            ));
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    private LocalDateTime parseDate(String date, boolean isStart) {
        if (date == null) {
            return isStart ? LocalDateTime.now() : LocalDateTime.of(9999, 12, 31, 23, 59);
        }
        try {
            return LocalDateTime.parse(date, FORMATTER);
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(date, FORMATTER_ISO);
            } catch (DateTimeParseException e2) {
                throw new BadRequestException("Invalid date format: " + date + ". Expected yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    private Event findEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + categoryId + " не найдена"));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    private List<EventShortDto> toShortDtos(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEventIds(eventIds);
        Map<Long, Long> viewsMap = getViewsBatch(eventIds);

        return events.stream()
                .map(event -> EventMapper.toShortDto(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)
                ))
                .toList();
    }

    private List<EventFullDto> toFullDtos(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedMap = requestRepository.countConfirmedByEventIds(eventIds);
        Map<Long, Long> viewsMap = getViewsBatch(eventIds);

        return events.stream()
                .map(event -> EventMapper.toFullDto(
                        event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)
                ))
                .toList();
    }

    private EventFullDto toFullDto(Event event) {
        Long confirmed = requestRepository.countConfirmedByEventId(event.getId());
        Long views = getViews(event.getId());
        return EventMapper.toFullDto(event, confirmed, views);
    }

    public Map<Long, Long> getViewsBatch(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        List<String> uris = eventIds.stream()
                .map(id -> "/events/" + id)
                .toList();

        List<ViewStatsDto> stats = statsClient.getStats(
                LocalDateTime.of(1970, 1, 1, 0, 0),
                LocalDateTime.now(),
                uris,
                true
        );

        return stats.stream()
                .collect(Collectors.toMap(
                        stat -> Long.parseLong(stat.getUri().replace("/events/", "")),
                        ViewStatsDto::getHits,
                        (a, b) -> a
                ));
    }

    private long getViews(Long eventId) {
        return getViewsBatch(List.of(eventId)).getOrDefault(eventId, 0L);
    }
}