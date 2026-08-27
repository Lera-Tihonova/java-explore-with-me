package ru.practicum.stats.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class StatsClient {

    private final RestTemplate restTemplate;
    private final String serverUrl;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(@Value("${stats-server.url}") String serverUrl) {
        this.serverUrl = serverUrl;
        this.restTemplate = new RestTemplate();
    }

    public void hit(EndpointHitDto hitDto) {
        log.info("Отправка статистики: app={}, uri={}, ip={}", hitDto.getApp(), hitDto.getUri(), hitDto.getIp());

        try {
            String url = serverUrl + "/hit";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<EndpointHitDto> requestEntity = new HttpEntity<>(hitDto, headers);
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Статистика успешно отправлена");
            } else {
                log.error("Ошибка при отправке статистики: {}", response.getStatusCode());
                throw new RuntimeException("Ошибка отправки статистики: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове сервиса статистики: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось сохранить статистику", e);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.info("Запрос статистики: start={}, end={}, uris={}, unique={}", start, end, uris, unique);

        try {
            String encodedStart = URLEncoder.encode(start.format(FORMATTER), StandardCharsets.UTF_8);
            String encodedEnd = URLEncoder.encode(end.format(FORMATTER), StandardCharsets.UTF_8);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(serverUrl + "/stats")
                    .queryParam("start", encodedStart)
                    .queryParam("end", encodedEnd);

            if (uris != null && !uris.isEmpty()) {
                builder.queryParam("uris", uris.toArray());
            }

            if (unique != null) {
                builder.queryParam("unique", unique);
            }

            String url = builder.build().encode().toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ParameterizedTypeReference<List<ViewStatsDto>> responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    responseType
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Получена статистика: {} записей", response.getBody().size());
                return response.getBody();
            } else {
                log.error("Ошибка при получении статистики: {}", response.getStatusCode());
                return List.of();
            }
        } catch (Exception e) {
            log.error("Ошибка при вызове сервиса статистики: {}", e.getMessage(), e);
            return List.of();
        }
    }
}