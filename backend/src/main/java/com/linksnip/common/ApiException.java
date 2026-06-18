package com.linksnip.common;

import org.springframework.http.HttpStatus;

/** Base for expected, mapped-to-HTTP errors. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
