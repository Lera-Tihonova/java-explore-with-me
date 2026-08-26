package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.*;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository requestRepository;
    private final StatsClient statsClient;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto request) {
        log.info("Создание события пользователем userId={}", userId);

        // ========== ВАЛИДАЦИЯ ==========
        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Лимит участников не может быть отрицательным");
        }
        if (request.getParticipantLimit() != null && request.getParticipantLimit() > 10000) {
            throw new IllegalArgumentException("Лимит участников не может превышать 10000");
        }
        // ===============================

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Category category = categoryRepository.findById(request.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));

        if (request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Дата события должна быть не раньше чем через 2 часа");
        }

        Event event = EventMapper.toEntity(request, initiator, category);
        event.setState(EventState.PENDING);
        event.setCreatedOn(LocalDateTime.now());

        if (request.getPaid() == null) {
            event.setPaid(false);
        }
        if (request.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (request.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }

        Event savedEvent = eventRepository.save(event);
        return EventMapper.toFullDto(savedEvent, 0L, 0L);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, int from, int size) {
        log.info("Получение событий пользователя userId={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        return events.stream()
                .map(event -> {
                    Long confirmed = requestRepository.countConfirmedByEventId(event.getId());
                    Long views = getViewsCount(event.getId());
                    return EventMapper.toShortDto(event, confirmed, views);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventByUser(Long userId, Long eventId) {
        log.info("Получение события userId={}, eventId={}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("Пользователь не является инициатором события");
        }

        Long confirmed = requestRepository.countConfirmedByEventId(eventId);
        Long views = getViewsCount(eventId);
        return EventMapper.toFullDto(event, confirmed, views);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Обновление события userId={}, eventId={}", userId, eventId);

        // ========== ВАЛИДАЦИЯ ==========
        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Лимит участников не может быть отрицательным");
        }
        if (request.getParticipantLimit() != null && request.getParticipantLimit() > 10000) {
            throw new IllegalArgumentException("Лимит участников не может превышать 10000");
        }

        if (request.getTitle() != null) {
            if (request.getTitle().length() < 3 || request.getTitle().length() > 120) {
                throw new IllegalArgumentException("Заголовок должен быть от 3 до 120 символов");
            }
        }

        if (request.getAnnotation() != null) {
            if (request.getAnnotation().length() < 20 || request.getAnnotation().length() > 2000) {
                throw new IllegalArgumentException("Аннотация должна быть от 20 до 2000 символов");
            }
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() < 20 || request.getDescription().length() > 7000) {
                throw new IllegalArgumentException("Описание должно быть от 20 до 7000 символов");
            }
        }
        // ===============================

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new IllegalArgumentException("Пользователь не является инициатором события");
        }

        if (event.getState() == EventState.PUBLISHED) {
            throw new IllegalArgumentException("Нельзя изменить опубликованное событие");
        }

        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new IllegalArgumentException("Дата события должна быть не раньше чем через 2 часа");
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));
            event.setCategory(category);
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

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("SEND_TO_REVIEW")) {
                event.setState(EventState.PENDING);
            } else if (request.getStateAction().equals("CANCEL_REVIEW")) {
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Long confirmed = requestRepository.countConfirmedByEventId(eventId);
        Long views = getViewsCount(eventId);
        return EventMapper.toFullDto(updatedEvent, confirmed, views);
    }

    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        log.info("Получение событий для администратора");

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now().minusYears(100);
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        List<EventState> stateList = states != null ?
                states.stream().map(EventState::valueOf).collect(Collectors.toList()) : null;

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Event> events = eventRepository.findAllByAdmin(users, stateList, categories, rangeStart, rangeEnd, pageable);

        return events.stream()
                .map(event -> {
                    Long confirmed = requestRepository.countConfirmedByEventId(event.getId());
                    Long views = getViewsCount(event.getId());
                    return EventMapper.toFullDto(event, confirmed, views);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события администратором eventId={}", eventId);

        // ========== ВАЛИДАЦИЯ ==========
        if (request.getParticipantLimit() != null && request.getParticipantLimit() < 0) {
            throw new IllegalArgumentException("Лимит участников не может быть отрицательным");
        }
        if (request.getParticipantLimit() != null && request.getParticipantLimit() > 10000) {
            throw new IllegalArgumentException("Лимит участников не может превышать 10000");
        }

        if (request.getTitle() != null) {
            if (request.getTitle().length() < 3 || request.getTitle().length() > 120) {
                throw new IllegalArgumentException("Заголовок должен быть от 3 до 120 символов");
            }
        }

        if (request.getAnnotation() != null) {
            if (request.getAnnotation().length() < 20 || request.getAnnotation().length() > 2000) {
                throw new IllegalArgumentException("Аннотация должна быть от 20 до 2000 символов");
            }
        }

        if (request.getDescription() != null) {
            if (request.getDescription().length() < 20 || request.getDescription().length() > 7000) {
                throw new IllegalArgumentException("Описание должно быть от 20 до 7000 символов");
            }
        }

        if (request.getEventDate() != null &&
                request.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
            throw new IllegalArgumentException("Дата события должна быть не раньше чем через 1 час");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        // Обновление полей
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Категория с id " + request.getCategory() + " не найдена"));
            event.setCategory(category);
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

        if (request.getStateAction() != null) {
            if (request.getStateAction().equals("PUBLISH_EVENT")) {
                if (event.getState() != EventState.PENDING) {
                    throw new IllegalArgumentException("Событие можно опубликовать только в статусе PENDING");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (request.getStateAction().equals("REJECT_EVENT")) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new IllegalArgumentException("Нельзя отклонить опубликованное событие");
                }
                event.setState(EventState.CANCELED);
            }
        }

        Event updatedEvent = eventRepository.save(event);
        Long confirmed = requestRepository.countConfirmedByEventId(eventId);
        Long views = getViewsCount(eventId);
        return EventMapper.toFullDto(updatedEvent, confirmed, views);
    }

    @Override
    public List<EventShortDto> getEventsForPublic(String text, List<Long> categories, Boolean paid,
                                                  LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable, String sort, int from, int size) {
        log.info("Получение событий для публичного доступа");

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }
        if (rangeEnd == null) {
            rangeEnd = LocalDateTime.now().plusYears(100);
        }

        Sort sortBy;
        if (sort != null && sort.equals("VIEWS")) {
            sortBy = Sort.by("views").descending();
        } else {
            sortBy = Sort.by("eventDate").ascending();
        }

        Pageable pageable = PageRequest.of(from / size, size, sortBy);
        Page<Event> events = eventRepository.findAllByPublic(text, categories, paid, rangeStart, rangeEnd,
                onlyAvailable != null && onlyAvailable, pageable);

        return events.stream()
                .map(event -> {
                    Long confirmed = requestRepository.countConfirmedByEventId(event.getId());
                    Long views = getViewsCount(event.getId());
                    return EventMapper.toShortDto(event, confirmed, views);
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventForPublic(Long eventId) {
        log.info("Получение события для публичного доступа eventId={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }

        Long confirmed = requestRepository.countConfirmedByEventId(eventId);
        Long views = getViewsCount(eventId);
        return EventMapper.toFullDto(event, confirmed, views);
    }

    private Long getViewsCount(Long eventId) {
        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(100),
                    LocalDateTime.now(),
                    List.of("/events/" + eventId),
                    false
            );
            return stats.isEmpty() ? 0L : stats.get(0).getHits();
        } catch (Exception e) {
            log.warn("Не удалось получить статистику для события {}", eventId);
            return 0L;
        }
    }
}