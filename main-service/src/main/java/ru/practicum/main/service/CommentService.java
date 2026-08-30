package ru.practicum.main.service;

import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.NewCommentDto;
import ru.practicum.main.dto.UpdateCommentDto;

import java.util.List;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto request);

    List<CommentDto> getCommentsByEvent(Long eventId, int from, int size);

    List<CommentDto> getCommentsByUser(Long userId, int from, int size);

    CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto request);

    void deleteComment(Long userId, Long commentId);
}