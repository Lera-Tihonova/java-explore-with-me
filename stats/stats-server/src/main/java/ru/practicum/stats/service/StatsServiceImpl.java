package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.mapper.StatsMapper;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        log.info("Сохранение информации о запросе: {}", hitDto);
        EndpointHit hit = StatsMapper.toEntity(hitDto);
        statsRepository.save(hit);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Получение статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Дата начала не может быть позже даты окончания");
        }

        if (unique) {
            return statsRepository.getStatsUnique(start, end, uris);
        } else {
            return statsRepository.getStats(start, end, uris);
        }
    }
}