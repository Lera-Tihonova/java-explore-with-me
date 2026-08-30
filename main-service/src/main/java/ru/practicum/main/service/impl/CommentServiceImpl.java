package ru.practicum.main.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.CommentDto;
import ru.practicum.main.dto.NewCommentDto;
import ru.practicum.main.dto.UpdateCommentDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.mapper.CommentMapper;
import ru.practicum.main.model.Comment;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CommentRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto request) {
        log.debug("Создание комментария пользователем userId={} к событию eventId={}", userId, eventId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = Comment.builder()
                .text(request.getText())
                .author(author)
                .event(event)
                .createdAt(LocalDateTime.now())
                .build();

        Comment saved = commentRepository.save(comment);
        log.debug("Создан комментарий id={}", saved.getId());

        return CommentMapper.toDto(saved);
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        log.debug("Получение комментариев к событию eventId={}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Событие с id " + eventId + " не найдено");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> page = commentRepository.findByEventId(eventId, pageable);

        return page.stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    public List<CommentDto> getCommentsByUser(Long userId, int from, int size) {
        log.debug("Получение комментариев пользователя userId={}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);
        Page<Comment> page = commentRepository.findByAuthorId(userId, pageable);

        return page.stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, UpdateCommentDto request) {
        log.debug("Обновление комментария userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id " + commentId + " не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является автором комментария");
        }

        comment.setText(request.getText());
        comment.setUpdatedAt(LocalDateTime.now());

        Comment updated = commentRepository.save(comment);
        log.debug("Обновлен комментарий id={}", updated.getId());

        return CommentMapper.toDto(updated);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.debug("Удаление комментария userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id " + commentId + " не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Пользователь не является автором комментария");
        }

        commentRepository.delete(comment);
        log.debug("Удален комментарий id={}", commentId);
    }
}