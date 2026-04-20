package com.chronicle.adapters.in.web;

import com.chronicle.adapters.in.web.dto.ErrorResponseDto;
import com.chronicle.application.exception.ProcessNotFoundException;
import com.chronicle.application.exception.SemanticValidationException;
import com.chronicle.domain.transition.InvalidTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(ProcessNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProcessNotFound(ProcessNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of("PROCESS_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidTransition(InvalidTransitionException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.of("INVALID_STATE_TRANSITION", exception.getMessage()));
    }

    @ExceptionHandler(SemanticValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleSemanticValidation(SemanticValidationException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponseDto.of(exception.code(), exception.getMessage(), exception.details()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of("INVALID_REQUEST", exception.getMessage(), extractDetails(exception)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of("INTERNAL_ERROR", "Unexpected server error."));
    }

    private Map<String, Object> extractDetails(Exception exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
            methodArgumentNotValidException.getBindingResult().getFieldErrors()
                    .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        } else if (exception instanceof BindException bindException) {
            bindException.getBindingResult().getFieldErrors()
                    .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
        }
        return details;
    }
}
