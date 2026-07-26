package com.elpandor.hlh.modules.automatisationzino.domain.exception;

public class TelechargementException extends RuntimeException {

    public TelechargementException(String message) {
        super(message);
    }

    public TelechargementException(String message, Throwable cause) {
        super(message, cause);
    }
}
