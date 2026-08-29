package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.ParticipationRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterIdOrderByCreatedAsc(Long userId);

    List<ParticipationRequest> findByEventIdOrderByCreatedAsc(Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long userId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    @Query("SELECT COUNT(r) FROM ParticipationRequest r WHERE r.event.id = :eventId AND r.status = 'CONFIRMED'")
    long countConfirmedByEventId(@Param("eventId") Long eventId);

    @Query("SELECT r.event.id, COUNT(r) FROM ParticipationRequest r " +
            "WHERE r.event.id IN :eventIds AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    List<Object[]> countConfirmedByEventIdsRaw(@Param("eventIds") List<Long> eventIds);

    default Map<Long, Long> countConfirmedByEventIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return countConfirmedByEventIdsRaw(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}