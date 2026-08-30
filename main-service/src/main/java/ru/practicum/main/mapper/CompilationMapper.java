package ru.practicum.main.mapper;

import ru.practicum.main.dto.CompilationDto;
import ru.practicum.main.dto.EventShortDto;
import ru.practicum.main.dto.NewCompilationDto;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;

import java.util.Set;
import java.util.stream.Collectors;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    public static Compilation toEntity(
            NewCompilationDto dto,
            Set<Event> events
    ) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned() != null && dto.getPinned())
                .events(events)
                .build();
    }

    public static CompilationDto toDto(
            Compilation compilation,
            Set<EventShortDto> eventDtos
    ) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }
}