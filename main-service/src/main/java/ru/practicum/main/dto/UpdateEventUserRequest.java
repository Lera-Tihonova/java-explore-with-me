package ru.practicum.main.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventUserRequest {

    private String annotation;
    private Long category;
    private String description;
    private LocalDateTime eventDate;
    private Location location;
    private Boolean paid;

    @PositiveOrZero
    private Integer participantLimit;

    private Boolean requestModeration;
    private String stateAction;
    private String title;

    @JsonCreator
    public UpdateEventUserRequest(
            @JsonProperty("annotation") String annotation,
            @JsonProperty("category") Long category,
            @JsonProperty("description") String description,
            @JsonProperty("eventDate") LocalDateTime eventDate,
            @JsonProperty("location") Location location,
            @JsonProperty("paid") Object paid,
            @JsonProperty("participantLimit") Object participantLimit,
            @JsonProperty("requestModeration") Object requestModeration,
            @JsonProperty("stateAction") String stateAction,
            @JsonProperty("title") String title) {
        this.annotation = annotation;
        this.category = category;
        this.description = description;
        this.eventDate = eventDate;
        this.location = location;
        this.paid = convertToBoolean(paid);
        this.participantLimit = convertToInteger(participantLimit);
        this.requestModeration = convertToBoolean(requestModeration);
        this.stateAction = stateAction;
        this.title = title;

        // ========== ВАЛИДАЦИЯ В КОНСТРУКТОРЕ ==========
        if (this.participantLimit != null && this.participantLimit < 0) {
            throw new IllegalArgumentException("Лимит участников не может быть отрицательным");
        }
        if (this.participantLimit != null && this.participantLimit > 10000) {
            throw new IllegalArgumentException("Лимит участников не может превышать 10000");
        }
        // ==============================================
    }

    private Boolean convertToBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            String str = ((String) value).trim().toLowerCase();
            return "true".equals(str) || "false".equals(str) ? Boolean.valueOf(str) : null;
        }
        return null;
    }

    private Integer convertToInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}