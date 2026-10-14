package com.instakill.common.error;

public class BadRequestException extends InstakillException {
    public BadRequestException(String message) {
        super(message);
    }
}
