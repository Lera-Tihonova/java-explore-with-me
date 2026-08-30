package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEventId(Long eventId, Pageable pageable);

    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId ORDER BY c.createdAt ASC")
    List<Comment> findByEventIdOrderByCreatedAtAsc(@Param("eventId") Long eventId);
}