package ru.practicum.main.mapper;

import ru.practicum.main.dto.EventFullDto;
import ru.practicum.main.dto.EventShortDto;
import ru.practicum.main.dto.Location;
import ru.practicum.main.dto.NewEventDto;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.User;

public final class EventMapper {

    private EventMapper() {
    }

    public static Event toEntity(
            NewEventDto dto,
            User initiator,
            Category category
    ) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .initiator(initiator)
                .location(new ru.practicum.main.model.Location(
                        dto.getLocation().getLat(),
                        dto.getLocation().getLon()
                ))
                .paid(dto.getPaid() != null && dto.getPaid())
                .participantLimit(dto.getParticipantLimit() == null
                        ? 0
                        : dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration() == null
                        || dto.getRequestModeration())
                .title(dto.getTitle())
                .build();
    }

    public static EventFullDto toFullDto(
            Event event,
            Long confirmedRequests,
            Long views
    ) {
        Location location = new Location(
                event.getLocation().getLat(),
                event.getLocation().getLon()
        );

        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toShortDto(event.getInitiator()))
                .location(location)
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState().name())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static EventShortDto toShortDto(
            Event event,
            Long confirmedRequests,
            Long views
    ) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }
}