package com.elpandor.hlh.common.core.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Clock;
import java.time.Instant;


public class ExceptionHandlingAdvice {
    /*private final Clock clock = Clock.systemDefaultZone();

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return handleException(ex.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> handleException(String message, HttpStatus httpStatus) {
        log.error("Error: " + message);
        ErrorResponse errorResponse = ErrorResponse.of(Instant.now(clock), message);
        return new ResponseEntity<>(errorResponse, httpStatus);
    }*/
}

