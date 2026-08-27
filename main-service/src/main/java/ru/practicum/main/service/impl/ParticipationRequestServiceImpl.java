package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.ParticipationRequestDto;
import ru.practicum.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.ParticipationRequestMapper;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        log.info("Создание запроса на участие userId={}, eventId={}", userId, eventId);

        // ========== ЗАМЕЧАНИЕ РЕВЬЮЕРА №2: ПРОВЕРКА eventId ==========
        if (eventId == null) {
            throw new IllegalArgumentException("eventId обязателен для создания запроса");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может создать запрос на участие");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByRequesterIdAndEventIdAndStatus(userId, eventId, "PENDING") ||
                requestRepository.existsByRequesterIdAndEventIdAndStatus(userId, eventId, "CONFIRMED")) {
            throw new ConflictException("Запрос уже существует");
        }

        Long confirmedCount = requestRepository.countConfirmedByEventId(eventId);
        if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        // ========== ЗАМЕЧАНИЕ РЕВЬЮЕРА №3: СТАТУС CONFIRMED ПРИ participantLimit == 0 ==========
        String status;
        if (event.getParticipantLimit() == 0) {
            status = "CONFIRMED";
        } else {
            status = event.getRequestModeration() ? "PENDING" : "CONFIRMED";
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        ParticipationRequest saved = requestRepository.save(request);
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        log.info("Получение запросов пользователя userId={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        return requestRepository.findByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена запроса userId={}, requestId={}", userId, requestId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id " + requestId + " не найден"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Запрос не принадлежит пользователю");
        }

        if (request.getStatus().equals("CANCELED") || request.getStatus().equals("REJECTED")) {
            throw new ConflictException("Запрос уже был отменён или отклонён");
        }

        request.setStatus("CANCELED");
        ParticipationRequest updated = requestRepository.save(request);
        return ParticipationRequestMapper.toDto(updated);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии userId={}, eventId={}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return requestRepository.findByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request) {
        log.info("Обновление статуса запросов userId={}, eventId={}", userId, eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            throw new ConflictException("Модерация запросов не требуется");
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());

        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        Long confirmedCount = requestRepository.countConfirmedByEventId(eventId);
        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest pr : requests) {
            if (!pr.getStatus().equals("PENDING")) {
                throw new ConflictException("Запрос должен быть в статусе PENDING");
            }
            if (!pr.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Запрос не относится к данному событию");
            }

            if (request.getStatus().equals("CONFIRMED")) {
                if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                    pr.setStatus("REJECTED");
                    rejectedRequests.add(pr);
                } else {
                    pr.setStatus("CONFIRMED");
                    confirmedRequests.add(pr);
                    confirmedCount++;
                }
            } else if (request.getStatus().equals("REJECTED")) {
                pr.setStatus("REJECTED");
                rejectedRequests.add(pr);
            }
        }

        if (request.getStatus().equals("CONFIRMED") &&
                event.getParticipantLimit() > 0 &&
                confirmedCount >= event.getParticipantLimit()) {

            List<ParticipationRequest> remaining = requestRepository.findByEventId(eventId).stream()
                    .filter(r -> r.getStatus().equals("PENDING"))
                    .collect(Collectors.toList());

            for (ParticipationRequest r : remaining) {
                r.setStatus("REJECTED");
                rejectedRequests.add(r);
            }
        }

        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmedRequests.stream()
                        .map(ParticipationRequestMapper::toDto)
                        .collect(Collectors.toList()))
                .rejectedRequests(rejectedRequests.stream()
                        .map(ParticipationRequestMapper::toDto)
                        .collect(Collectors.toList()))
                .build();
    }
}