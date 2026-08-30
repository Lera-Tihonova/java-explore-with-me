package ru.practicum.main.mapper;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.model.Comment;

public final class CommentMapper {

    private CommentMapper() {
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