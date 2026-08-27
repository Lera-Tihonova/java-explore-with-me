package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByCategory(Category category);

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    @Query("SELECT e FROM Event e " +
            "WHERE (:users IS NULL OR e.initiator.id IN :users) " +
            "AND (:states IS NULL OR e.state IN :states) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (e.eventDate >= :rangeStart) " +
            "AND (e.eventDate <= :rangeEnd)")
    Page<Event> findAllByAdmin(@Param("users") List<Long> users,
                               @Param("states") List<EventState> states,
                               @Param("categories") List<Long> categories,
                               @Param("rangeStart") LocalDateTime rangeStart,
                               @Param("rangeEnd") LocalDateTime rangeEnd,
                               Pageable pageable);

    @Query(value = "SELECT * FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.event_date >= CAST(:rangeStart AS TIMESTAMP) " +
            "AND e.event_date <= CAST(:rangeEnd AS TIMESTAMP) " +
            "AND (:text IS NULL OR :text = '' OR " +
            "     CAST(e.annotation AS TEXT) ILIKE CONCAT('%', CAST(:text AS TEXT), '%') OR " +
            "     CAST(e.description AS TEXT) ILIKE CONCAT('%', CAST(:text AS TEXT), '%')) " +
            "AND (:categories IS NULL OR :categories = '' OR " +
            "     CAST(e.category_id AS TEXT) = ANY(STRING_TO_ARRAY(CAST(:categories AS TEXT), ','))) " +
            "AND (:paid IS NULL OR e.paid = CAST(:paid AS BOOLEAN)) " +
            "AND (:onlyAvailable = false OR " +
            "     e.participant_limit = 0 OR " +
            "     (SELECT COUNT(*) FROM participation_requests pr " +
            "      WHERE pr.event_id = e.id AND pr.status = 'CONFIRMED') < e.participant_limit) " +
            "ORDER BY e.event_date ASC",
            nativeQuery = true)
    Page<Event> findAllByPublicNative(@Param("text") String text,
                                      @Param("categories") String categories,
                                      @Param("paid") Boolean paid,
                                      @Param("rangeStart") LocalDateTime rangeStart,
                                      @Param("rangeEnd") LocalDateTime rangeEnd,
                                      @Param("onlyAvailable") Boolean onlyAvailable,
                                      Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM events e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND e.event_date >= CAST(:rangeStart AS TIMESTAMP) " +
            "AND e.event_date <= CAST(:rangeEnd AS TIMESTAMP) " +
            "AND (:text IS NULL OR :text = '' OR " +
            "     CAST(e.annotation AS TEXT) ILIKE CONCAT('%', CAST(:text AS TEXT), '%') OR " +
            "     CAST(e.description AS TEXT) ILIKE CONCAT('%', CAST(:text AS TEXT), '%')) " +
            "AND (:categories IS NULL OR :categories = '' OR " +
            "     CAST(e.category_id AS TEXT) = ANY(STRING_TO_ARRAY(CAST(:categories AS TEXT), ','))) " +
            "AND (:paid IS NULL OR e.paid = CAST(:paid AS BOOLEAN)) " +
            "AND (:onlyAvailable = false OR " +
            "     e.participant_limit = 0 OR " +
            "     (SELECT COUNT(*) FROM participation_requests pr " +
            "      WHERE pr.event_id = e.id AND pr.status = 'CONFIRMED') < e.participant_limit)",
            nativeQuery = true)
    Long countAllByPublicNative(@Param("text") String text,
                                @Param("categories") String categories,
                                @Param("paid") Boolean paid,
                                @Param("rangeStart") LocalDateTime rangeStart,
                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                @Param("onlyAvailable") Boolean onlyAvailable);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    Long countConfirmedRequests(@Param("eventId") Long eventId);
}