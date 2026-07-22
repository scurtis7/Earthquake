package com.scurtis.earthquakes.exception;

import com.scurtis.earthquakes.exception.model.ErrorResponse;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ValidationException.class)
    protected ResponseEntity<ErrorResponse> handleValidationException(ValidationException exception) {
        log.error("Handling ValidationException", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildErrorMessage(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), "ValidationException", exception.getMessage()));
    }

    @ExceptionHandler(value = ElasticsearchIndexException.class)
    protected ResponseEntity<ErrorResponse> handleElasticsearchIndexException(ElasticsearchIndexException exception) {
        log.error("Handling ElasticsearchIndexException", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(MediaType.APPLICATION_JSON)
            .body(buildErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR.value(), HttpStatus.INTERNAL_SERVER_ERROR.name(), "ElasticsearchIndexException", exception.getMessage()));
    }

    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ErrorResponse response = buildErrorMessage(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.name(), exception.getClass().getName(), exception.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    private ErrorResponse buildErrorMessage(int code, String status, String exception, String message) {
        return new ErrorResponse(code, status, exception, message, LocalDate.now());
    }

}
