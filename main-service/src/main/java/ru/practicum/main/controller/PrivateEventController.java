package ru.practicum.main.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.EventFullDto;
import ru.practicum.main.dto.EventShortDto;
import ru.practicum.main.dto.NewEventDto;
import ru.practicum.main.dto.UpdateEventUserRequest;
import ru.practicum.main.service.EventService;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(
            @PathVariable Long userId,
            @Valid @RequestBody NewEventDto request) {
        log.info("POST /users/{}/events - добавление нового события", userId);
        return eventService.createEvent(userId, request);
    }

    @GetMapping
    public List<EventShortDto> getEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /users/{}/events - получение событий пользователя", userId);
        return eventService.getEventsByUser(userId, from, size);
    }

    @GetMapping("/{eventId}")
    public EventFullDto getEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{} - получение события", userId, eventId);
        return eventService.getEventByUser(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventUserRequest request) {
        log.info("PATCH /users/{}/events/{} - обновление события", userId, eventId);
        return eventService.updateEventByUser(userId, eventId, request);
    }
}