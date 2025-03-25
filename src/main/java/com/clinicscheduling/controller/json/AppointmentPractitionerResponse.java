package com.janeapp.clinicscheduling.controller.json;

import com.janeapp.clinicscheduling.entity.AppointmentType;

public record AppointmentPractitionerResponse(PatientResponse patient,
                                              AppointmentType appointmentType,
                                              TimeSlotResponse time) {

}
