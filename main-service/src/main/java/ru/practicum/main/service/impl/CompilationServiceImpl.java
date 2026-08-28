package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.CompilationDto;
import ru.practicum.main.dto.NewCompilationDto;
import ru.practicum.main.dto.UpdateCompilationRequest;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.CompilationMapper;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.service.CompilationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final EventServiceImpl eventService;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        Set<Event> events = resolveEvents(request.getEvents());

        Compilation compilation = CompilationMapper.toEntity(request, events);

        return CompilationMapper.toDto(
                compilationRepository.save(compilation),
                requestRepository,
                eventService
        );
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(
            Long compId,
            UpdateCompilationRequest request
    ) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() ->
                        new NotFoundException("Подборка с id " + compId + " не найдена"));

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }

        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }

        if (request.getEvents() != null) {
            compilation.setEvents(resolveEvents(request.getEvents()));
        }

        return CompilationMapper.toDto(
                compilationRepository.save(compilation),
                requestRepository,
                eventService
        );
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() ->
                        new NotFoundException("Подборка с id " + compId + " не найдена"));

        compilationRepository.delete(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(
            Boolean pinned,
            int from,
            int size
    ) {
        Pageable pageable = PageRequest.of(from / size, size);

        if (pinned == null) {
            return compilationRepository.findAll(pageable)
                    .stream()
                    .map(comp -> CompilationMapper.toDto(comp, requestRepository, eventService))
                    .toList();
        }

        return compilationRepository.findByPinned(pinned, pageable)
                .stream()
                .map(comp -> CompilationMapper.toDto(comp, requestRepository, eventService))
                .toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() ->
                        new NotFoundException("Подборка с id " + compId + " не найдена"));

        return CompilationMapper.toDto(compilation, requestRepository, eventService);
    }

    private Set<Event> resolveEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Event> events = eventRepository.findAllById(eventIds);

        if (events.size() != eventIds.size()) {
            throw new NotFoundException("Некоторые события подборки не найдены");
        }

        return new HashSet<>(events);
    }
}