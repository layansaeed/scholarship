package com.example.beans.exception;

public class ExternalApiRetryException extends RuntimeException {

    public ExternalApiRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}