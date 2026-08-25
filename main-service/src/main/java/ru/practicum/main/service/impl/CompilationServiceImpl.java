package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ru.practicum.main.service.CompilationService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        log.info("Создание подборки: {}", request.getTitle());

        Set<Event> events = new HashSet<>();
        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
        }

        Compilation compilation = CompilationMapper.toEntity(request, events);
        Compilation savedCompilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        log.info("Обновление подборки с id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(request.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updatedCompilation = compilationRepository.save(compilation);
        return CompilationMapper.toDto(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id " + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        log.info("Получение списка подборок: pinned={}, from={}, size={}", pinned, from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        if (pinned != null) {
            return compilationRepository.findByPinned(pinned, pageable).stream()
                    .map(CompilationMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            return compilationRepository.findAll(pageable).stream()
                    .map(CompilationMapper::toDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
        return CompilationMapper.toDto(compilation);
    }
}