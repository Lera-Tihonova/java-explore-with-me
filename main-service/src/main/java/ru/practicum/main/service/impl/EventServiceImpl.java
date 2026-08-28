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
import ru.practicum.main.dto.UpdateEventAdminRequest;
import ru.practicum.main.dto.UpdateEventUserRequest;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.EventMapper;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.EventService;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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
    public EventFullDto createEvent(
            Long userId,
            NewEventDto request
    ) {
        log.debug("Создание события пользователем userId={}", userId);

        User initiator = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + userId + " не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() ->
                        new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));

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
    public List<EventShortDto> getEventsByUser(
            Long userId,
            int from,
            int size
    ) {
        log.debug("Получение событий пользователя userId={}", userId);
        ensureUserExists(userId);

        Pageable pageable = PageRequest.of(from / size, size);

        return eventRepository.findByInitiatorId(userId, pageable)
                .stream()
                .map(this::toShortDto)
                .toList();
    }

    @Override
    public EventFullDto getEventByUser(
            Long userId,
            Long eventId
    ) {
        log.debug("Получение события userId={}, eventId={}", userId, eventId);

        Event event = findEvent(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(
            Long userId,
            Long eventId,
            UpdateEventUserRequest request
    ) {
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

        if ("SEND_TO_REVIEW".equals(request.getStateAction())) {
            event.setState(EventState.PENDING);
        } else if ("CANCEL_REVIEW".equals(request.getStateAction())) {
            event.setState(EventState.CANCELED);
        }

        Event updated = eventRepository.save(event);
        log.debug("Событие обновлено id={}", updated.getId());

        return toFullDto(updated);
    }

    @Override
    public List<EventFullDto> getEventsForAdmin(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    ) {
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

        return eventRepository.findAllByAdmin(
                        users,
                        eventStates,
                        categories,
                        start,
                        end,
                        pageable
                )
                .stream()
                .map(this::toFullDto)
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(
            Long eventId,
            UpdateEventAdminRequest request
    ) {
        log.debug("Обновление события администратором eventId={}", eventId);

        Event event = findEvent(eventId);

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
            if (request.getEventDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Дата события не может быть в прошлом");
            }
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

        if ("PUBLISH_EVENT".equals(request.getStateAction())) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Событие можно опубликовать только в статусе PENDING");
            }
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if ("REJECT_EVENT".equals(request.getStateAction())) {
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
    public List<EventShortDto> getEventsForPublic(
            String text,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    ) {
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
        if ("EVENT_DATE".equalsIgnoreCase(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "eventDate");
        }

        Pageable pageable = PageRequest.of(from / size, size, sorting);

        Page<Event> eventPage = eventRepository.findAllByPublic(
                categories == null || categories.isEmpty() ? null : categories,
                paid,
                start,
                end,
                Boolean.TRUE.equals(onlyAvailable),
                pageable
        );

        log.debug("Найдено событий: {}", eventPage.getTotalElements());

        return eventPage.stream()
                .map(this::toShortDto)
                .toList();
    }

    @Override
    public EventFullDto getEventForPublic(
            Long eventId,
            HttpServletRequest request
    ) {
        log.debug("Получение события для публичного доступа eventId={}", eventId);

        Event event = findEvent(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие с id " + eventId + " не опубликовано");
        }

        try {
            String clientIp = request.getRemoteAddr();
            log.debug("Сохранение статистики для события {}, IP={}", eventId, clientIp);

            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri("/events/" + eventId)
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now())
                    .build();

            statsClient.hit(hitDto);
            log.debug("Статистика сохранена для события {}", eventId);
        } catch (Exception e) {
            log.warn("Не удалось сохранить статистику для события {}: {}", eventId, e.getMessage());
        }

        return toFullDto(event);
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
                .orElseThrow(() ->
                        new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new NotFoundException("Категория с id " + categoryId + " не найдена"));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    private EventShortDto toShortDto(Event event) {
        long confirmed = requestRepository.countConfirmedByEventId(event.getId());
        long views = getViews(event.getId());
        return EventMapper.toShortDto(event, confirmed, views);
    }

    private EventFullDto toFullDto(Event event) {
        long confirmed = requestRepository.countConfirmedByEventId(event.getId());
        long views = getViews(event.getId());
        return EventMapper.toFullDto(event, confirmed, views);
    }

    public long getViews(Long eventId) {
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.of(1970, 1, 1, 0, 0),
                    LocalDateTime.now(),
                    List.of("/events/" + eventId),
                    false  // ← ИСПРАВЛЕНО: unique = false
            );

            return stats.stream()
                    .filter(item -> ("/events/" + eventId).equals(item.getUri()))
                    .mapToLong(ViewStatsDto::getHits)
                    .sum();
        } catch (Exception e) {
            return 0L;
        }
    }
}