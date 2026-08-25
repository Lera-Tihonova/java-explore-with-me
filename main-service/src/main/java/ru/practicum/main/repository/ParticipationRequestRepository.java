package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long userId);

    List<ParticipationRequest> findByEventId(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    Optional<ParticipationRequest> findByIdAndEventId(Long requestId, Long eventId);

    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, String status);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    Long countConfirmedByEventId(@Param("eventId") Long eventId);
}