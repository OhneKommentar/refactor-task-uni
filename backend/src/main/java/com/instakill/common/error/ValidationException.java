package com.instakill.common.error;

import java.util.Map;

public class ValidationException extends InstakillException {
    private final Map<String, String> errors;

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, String> errors() {
        return errors;
    }
}
