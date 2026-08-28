package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository
        extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterIdOrderByCreatedAsc(Long userId);

    List<ParticipationRequest> findByEventIdOrderByCreatedAsc(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    @Query("""
            select count(r)
            from ParticipationRequest r
            where r.event.id = :eventId
              and r.status = 'CONFIRMED'
            """)
    long countConfirmedByEventId(@Param("eventId") Long eventId);
}