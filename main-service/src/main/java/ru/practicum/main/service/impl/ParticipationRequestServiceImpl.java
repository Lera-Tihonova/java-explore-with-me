package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.ParticipationRequestDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.ParticipationRequestMapper;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.ParticipationRequest;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl
        implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId обязателен");
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() ->
                        new NotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор события не может создать запрос на участие");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Запрос уже существует");
        }

        long confirmed = requestRepository.countConfirmedByEventId(eventId);

        if (event.getParticipantLimit() > 0 && confirmed >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        String status = event.getParticipantLimit() == 0 || !event.getRequestModeration()
                ? "CONFIRMED"
                : "PENDING";

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(status)
                .build();

        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        return requestRepository
                .findByRequesterIdOrderByCreatedAsc(userId)
                .stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(
            Long userId,
            Long requestId
    ) {
        ParticipationRequest request = requestRepository
                .findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() ->
                        new NotFoundException("Запрос с id " + requestId + " не найден"));

        if ("CANCELED".equals(request.getStatus())) {
            return ParticipationRequestMapper.toDto(request);
        }

        request.setStatus("CANCELED");
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(
            Long userId,
            Long eventId
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        return requestRepository
                .findByEventIdOrderByCreatedAsc(eventId)
                .stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest request
    ) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является инициатором события");
        }

        String targetStatus = request.getStatus();

        if (!"CONFIRMED".equals(targetStatus) && !"REJECTED".equals(targetStatus)) {
            throw new ConflictException("Некорректный статус запроса");
        }

        List<ParticipationRequest> requests =
                requestRepository.findAllById(request.getRequestIds());

        if (requests.size() != request.getRequestIds().size()) {
            throw new NotFoundException("Некоторые запросы не найдены");
        }

        for (ParticipationRequest item : requests) {
            if (!item.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Запрос не относится к данному событию");
            }

            if (!"PENDING".equals(item.getStatus())) {
                throw new ConflictException("Запрос должен быть в статусе PENDING");
            }
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        long confirmedCount = requestRepository.countConfirmedByEventId(eventId);

        for (ParticipationRequest item : requests) {
            if ("REJECTED".equals(targetStatus)) {
                item.setStatus("REJECTED");
                rejected.add(item);
                continue;
            }

            if (event.getParticipantLimit() > 0 && confirmedCount >= event.getParticipantLimit()) {
                item.setStatus("REJECTED");
                rejected.add(item);
            } else {
                item.setStatus("CONFIRMED");
                confirmed.add(item);
                confirmedCount++;
            }
        }

        requestRepository.saveAll(requests);

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(confirmed.stream()
                        .map(ParticipationRequestMapper::toDto)
                        .toList())
                .rejectedRequests(rejected.stream()
                        .map(ParticipationRequestMapper::toDto)
                        .toList())
                .build();
    }
}