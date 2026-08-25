package ru.practicum.main.service;

import ru.practicum.main.dto.*;
import java.time.LocalDateTime;
import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto request);

    List<EventShortDto> getEventsByUser(Long userId, int from, int size);

    EventFullDto getEventByUser(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventFullDto> getEventsForAdmin(List<Long> users, List<String> states, List<Long> categories,
                                         LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getEventsForPublic(String text, List<Long> categories, Boolean paid,
                                           LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                           Boolean onlyAvailable, String sort, int from, int size);

    EventFullDto getEventForPublic(Long eventId);
}