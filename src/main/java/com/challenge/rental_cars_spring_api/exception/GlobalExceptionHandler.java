package com.challenge.rental_cars_spring_api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ✅ Handler específico para FileSizeExceededException
    @ExceptionHandler(FileSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleFileSizeExceeded(FileSizeExceededException ex) {
        log.error("Arquivo muito grande: {}", ex.getMessage());

        Map<String, Object> errorResponse = Map.of(
                "error", "Tamanho de arquivo excedido",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ✅ Handler para MaxUploadSizeExceededException (do Spring)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.error("Arquivo muito grande (Spring): {}", ex.getMessage());

        Map<String, Object> errorResponse = Map.of(
                "error", "Tamanho de arquivo excedido",
                "message", "O tamanho máximo permitido é 10MB",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Argumento inválido: {}", ex.getMessage());

        Map<String, Object> errorResponse = Map.of(
                "error", "Requisição inválida",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Erro inesperado: ", ex);

        Map<String, Object> errorResponse = Map.of(
                "error", "Erro interno no servidor",
                "message", "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}