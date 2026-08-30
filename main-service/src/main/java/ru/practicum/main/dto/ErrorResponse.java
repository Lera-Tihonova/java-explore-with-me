package ru.practicum.main.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String status;
    private String reason;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;

    public static ErrorResponse badRequest(String message) {
        return ErrorResponse.builder()
                .status("BAD_REQUEST")
                .reason("Incorrectly made request.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse badRequest(String message, Map<String, String> errors) {
        return ErrorResponse.builder()
                .status("BAD_REQUEST")
                .reason("Incorrectly made request.")
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse notFound(String message) {
        return ErrorResponse.builder()
                .status("NOT_FOUND")
                .reason("The required object was not found.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse conflict(String message) {
        return ErrorResponse.builder()
                .status("CONFLICT")
                .reason("Integrity constraint has been violated.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse internalServerError(String message) {
        return ErrorResponse.builder()
                .status("INTERNAL_SERVER_ERROR")
                .reason("Internal server error.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}