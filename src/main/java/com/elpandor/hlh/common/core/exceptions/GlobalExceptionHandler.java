package com.elpandor.hlh.common.core.exceptions;

import com.elpandor.hlh.common.utils.ErrorResponseData;
import jakarta.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseData> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponseData error = ErrorResponseData.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .message("Donnée inexistante")
                .detail(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.OK);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseData> handleResourceNotFound(NotFoundException ex) {
        ErrorResponseData error = ErrorResponseData.builder()
                .code(HttpStatus.NOT_FOUND.value())
                .message("Donnée inexistante")
                .detail(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.OK);
    }

    @ExceptionHandler(InvalidEntityException.class)
    public ResponseEntity<ErrorResponseData> handleValidationError(InvalidEntityException ex) {
        ErrorResponseData error = ErrorResponseData.builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Données invalides")
                .detail(ex.getMessage())
                .erreurs(ex.getErrors())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseData> handleAccesDeniedError(AccessDeniedException ex) {
        ErrorResponseData error = ErrorResponseData.builder()
                .code(HttpStatus.UNAUTHORIZED.value())
                .message("Accès non autorisé")
                .detail("Vous n'avez pas les habilitations nécessaires pour éffectuer cette opération")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponseData> handleAllOtherError(Exception ex) {
        ex.printStackTrace();
        ErrorResponseData error = ErrorResponseData.builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Une erreur est survenue")
                .detail(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
