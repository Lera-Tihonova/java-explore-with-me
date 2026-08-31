package ru.practicum.main.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.CommentRequestDto;
import ru.practicum.main.service.CommentService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/users/{userId}/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody CommentRequestDto request
    ) {
        log.info("POST /users/{}/events/{}/comments - добавление комментария", userId, eventId);
        return commentService.createComment(userId, eventId, request);
    }

    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getCommentsByEvent(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /events/{}/comments - получение комментариев к событию", eventId);
        return commentService.getCommentsByEvent(eventId, from, size);
    }

    @GetMapping("/users/{userId}/comments")
    public List<CommentDto> getCommentsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET /users/{}/comments - получение комментариев пользователя", userId);
        return commentService.getCommentsByUser(userId, from, size);
    }

    @PatchMapping("/users/{userId}/comments/{commentId}")
    public CommentDto updateComment(
            @PathVariable Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequestDto request
    ) {
        log.info("PATCH /users/{}/comments/{} - обновление комментария", userId, commentId);
        return commentService.updateComment(userId, commentId, request);
    }

    @DeleteMapping("/users/{userId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId
    ) {
        log.info("DELETE /users/{}/comments/{} - удаление комментария", userId, commentId);
        commentService.deleteComment(userId, commentId);
    }
}