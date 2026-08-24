package ru.practicum.stats.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Ошибка валидации: {}", ex.getMessage(), ex);
        Map<String, Object> errors = new HashMap<>();
        errors.put("timestamp", LocalDateTime.now().toString());
        errors.put("status", "BAD_REQUEST");
        errors.put("reason", "Incorrectly made request.");
        errors.put("message", "Ошибка валидации полей");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        errors.put("errors", fieldErrors);

        return errors;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleConstraintViolation(ConstraintViolationException e) {
        log.error("Ошибка валидации: {}", e.getMessage(), e);
        return Map.of(
                "error", "Ошибка валидации",
                "message", e.getMessage()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return Map.of(
                "error", "Ошибка валидации",
                "message", e.getMessage()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleThrowable(Throwable e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return Map.of(
                "error", "Внутренняя ошибка сервера",
                "message", "Произошла непредвиденная ошибка"
        );
    }
}