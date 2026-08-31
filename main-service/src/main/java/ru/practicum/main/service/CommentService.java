package ru.practicum.main.service;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.CommentRequestDto;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, CommentRequestDto request);

    List<CommentDto> getCommentsByEvent(Long eventId, int from, int size);

    List<CommentDto> getCommentsByUser(Long userId, int from, int size);

    CommentDto updateComment(Long userId, Long commentId, CommentRequestDto request);

    void deleteComment(Long userId, Long commentId);
}