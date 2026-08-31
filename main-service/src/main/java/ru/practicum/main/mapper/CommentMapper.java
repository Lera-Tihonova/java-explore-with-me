package ru.practicum.main.mapper;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.model.Comment;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.User;

import java.time.LocalDateTime;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static Comment toEntity(User author, Event event, String text) {
        return Comment.builder()
                .text(text)
                .author(author)
                .event(event)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static CommentDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .author(UserMapper.toShortDto(comment.getAuthor()))
                .eventId(comment.getEvent().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}