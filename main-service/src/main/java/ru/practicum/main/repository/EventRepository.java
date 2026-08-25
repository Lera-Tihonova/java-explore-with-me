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

    // Добавленный метод для проверки наличия событий в категории
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

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = 'PUBLISHED' " +
            "AND (:text IS NULL OR LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) " +
            "AND (:categories IS NULL OR e.category.id IN :categories) " +
            "AND (:paid IS NULL OR e.paid = :paid) " +
            "AND (e.eventDate >= :rangeStart) " +
            "AND (e.eventDate <= :rangeEnd) " +
            "AND (:onlyAvailable = false OR e.participantLimit = 0 OR (SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = e.id AND r.status = 'CONFIRMED') < e.participantLimit)")
    Page<Event> findAllByPublic(@Param("text") String text,
                                @Param("categories") List<Long> categories,
                                @Param("paid") Boolean paid,
                                @Param("rangeStart") LocalDateTime rangeStart,
                                @Param("rangeEnd") LocalDateTime rangeEnd,
                                @Param("onlyAvailable") Boolean onlyAvailable,
                                Pageable pageable);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    Long countConfirmedRequests(@Param("eventId") Long eventId);
}