package com.janeapp.clinicscheduling.controller.json;

public record AppointmentRequest(
        long patientId,
        long practitionerId,
        String time,
        String type,
        String comment) {

}
