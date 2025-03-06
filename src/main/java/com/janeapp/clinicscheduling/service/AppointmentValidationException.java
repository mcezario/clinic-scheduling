package com.janeapp.clinicscheduling.service;

public class AppointmentValidationException extends RuntimeException {

    public AppointmentValidationException(String message) {
        super(message);
    }

    public AppointmentValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}
