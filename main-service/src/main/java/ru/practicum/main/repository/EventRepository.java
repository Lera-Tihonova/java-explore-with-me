package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    List<Event> findByCategoryId(Long categoryId);

    @Query("""
            select e
            from Event e
            where (:users is null or e.initiator.id in :users)
              and (:states is null or e.state in :states)
              and (:categories is null or e.category.id in :categories)
              and e.eventDate >= :rangeStart
              and e.eventDate <= :rangeEnd
            """)
    Page<Event> findAllByAdmin(
            @Param("users") List<Long> users,
            @Param("states") List<EventState> states,
            @Param("categories") List<Long> categories,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            Pageable pageable
    );

    @Query("""
            select e
            from Event e
            where e.state = ru.practicum.main.model.EventState.PUBLISHED
              and e.eventDate >= :rangeStart
              and e.eventDate <= :rangeEnd
              and (:text is null or lower(e.annotation) like lower(concat('%', :text, '%')) or lower(e.description) like lower(concat('%', :text, '%')))
              and (:categories is null or e.category.id in :categories)
              and (:paid is null or e.paid = :paid)
              and (:onlyAvailable = false
                   or e.participantLimit = 0
                   or (select count(r)
                       from ParticipationRequest r
                       where r.event.id = e.id
                         and r.status = 'CONFIRMED') < e.participantLimit)
            """)
    Page<Event> findAllByPublic(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            Pageable pageable
    );
}