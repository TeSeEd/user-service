package com.aston.userservice.exception;

import com.aston.userservice.dto.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NoSuchElementException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        new ErrorResponseDto(
                                404,
                                e.getMessage(),
                                LocalDateTime.now())
                );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponseDto(
                                400,
                                e.getMessage(),
                                LocalDateTime.now())
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException e) {
        return ResponseEntity
                .badRequest()
                .body(
                        new ErrorResponseDto(
                                400,
                                e.getBindingResult().getFieldErrors()
                                        .stream()
                                        .map(fieldError ->
                                                fieldError.getField() + ": " + fieldError.getDefaultMessage()
                                        )
                                        .collect(Collectors.joining("; ")),
                                LocalDateTime.now()
                        )
                );
    }
}
