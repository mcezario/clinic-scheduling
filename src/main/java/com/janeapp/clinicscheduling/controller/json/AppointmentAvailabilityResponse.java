package com.janeapp.clinicscheduling.controller.json;

import java.util.List;

public record AppointmentAvailabilityResponse(PractitionerResponse practitioner,
                                              List<TimeSlotResponse> availableSlots) {

}
