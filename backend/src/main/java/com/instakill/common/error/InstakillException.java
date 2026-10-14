package com.instakill.common.error;

public abstract class InstakillException extends RuntimeException {

    protected InstakillException(String message) {
        super(message);
    }

    protected InstakillException(String message, Throwable cause) {
        super(message, cause);
    }
}
