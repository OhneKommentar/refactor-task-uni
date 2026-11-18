package com.instakill.common.error;

public class ServiceUnavailableException extends InstakillException {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
