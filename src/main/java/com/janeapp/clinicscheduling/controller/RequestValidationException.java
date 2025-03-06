package com.janeapp.clinicscheduling.controller;

public class RequestValidationException extends RuntimeException {

    public RequestValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
