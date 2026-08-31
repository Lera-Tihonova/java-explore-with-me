package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.Comment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByEventId(Long eventId, Pageable pageable);

    Page<Comment> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.event.id = :eventId ORDER BY c.createdAt ASC")
    List<Comment> findByEventIdOrderByCreatedAtAsc(@Param("eventId") Long eventId);

    long countByEventId(Long eventId);

    @Query("SELECT c.event.id, COUNT(c) FROM Comment c WHERE c.event.id IN :eventIds GROUP BY c.event.id")
    List<Object[]> countByEventIdsRaw(@Param("eventIds") List<Long> eventIds);

    default Map<Long, Long> countByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return countByEventIdsRaw(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}