package com.elpandor.hlh.common.core.exceptions;

public enum ErrorCodes {

    NOT_FOUND(1000),
    NOT_VALID(2000),
    ALREADY_IN_USE(3000),

    ;


    private int code;

    ErrorCodes(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}