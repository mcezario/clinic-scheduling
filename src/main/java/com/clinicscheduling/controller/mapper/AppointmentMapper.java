package com.janeapp.clinicscheduling.controller.mapper;

import com.janeapp.clinicscheduling.controller.RequestValidationException;
import com.janeapp.clinicscheduling.controller.json.*;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Patient;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.repository.PatientRepository;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import com.janeapp.clinicscheduling.service.TimeSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Component
public class AppointmentMapper {

    @Autowired
    private AppointmentTypeMapper appointmentTypeMapper;

    @Autowired
    private PractitionerMapper practitionerMapper;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private PatientRepository patientRepository;

    public Appointment fromRequest(final AppointmentRequest request, final String timezone) {
        Instant scheduleTime = convertScheduleToInstant(request.time()).atZone(ZoneId.of(timezone)).toInstant();
        AppointmentType appointmentType = appointmentTypeMapper.fromString(request.type());
        Practitioner practitioner = practitionerMapper.fromId(request.practitionerId());
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new NoSuchElementException("Patient not found"));

        Appointment appointment = new Appointment();
        appointment.setType(appointmentType);
        appointment.setStartAt(scheduleTime);
        appointment.setEndAt(scheduleTime.plus(appointmentType.amount(), appointmentType.unit()));
        appointment.setPatient(patient);
        appointment.setPractitioner(practitioner);

        return appointment;
    }

    public AppointmentResponse toResponse(final Appointment appointment) {
        return new AppointmentResponse(appointment.getId());
    }

    public List<AppointmentPractitionerResponse> toResponse(final List<Appointment> appointments, final String timeZone) {
        return appointments
                .stream()
                .map(a -> {
                    PatientResponse patient = new PatientResponse(a.getPatient().fullName(), a.getPatient().getEmail(), a.getPatient().getPhone());
                    TimeSlotResponse timeSlotResponse = new TimeSlotResponse(formatDateTime(a.getStartAt(), timeZone), formatDateTime(a.getEndAt(), timeZone));
                    return new AppointmentPractitionerResponse(patient, a.getType(), timeSlotResponse);
                })
                .collect(Collectors.toList());
    }

    public List<AppointmentAvailabilityResponse> toResponse(final Map<Practitioner, List<TimeSlot>> appointments, final String timeZone) {
        return appointments.entrySet()
                .stream()
                .map(a -> {
                    Practitioner p = a.getKey();
                    PractitionerResponse practitionerResponse = new PractitionerResponse(p.getId(), p.fullName(), p.getEmail(), p.getPhone());
                    List<TimeSlotResponse> slots = a.getValue()
                            .stream().map(ts -> new TimeSlotResponse(formatDateTime(ts.start(), timeZone), formatDateTime(ts.end(), timeZone)))
                            .collect(Collectors.toList());
                    return new AppointmentAvailabilityResponse(practitionerResponse, slots);
                })
                .collect(Collectors.toList());
    }

    public String formatDateTime(final Instant time, final String timezone) {
        return time.atZone(ZoneId.of(timezone)).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private Instant convertScheduleToInstant(final String dateTime) {
        try {
            return Instant.parse(dateTime);
        } catch (DateTimeParseException e) {
            throw new RequestValidationException("Schedule time should be in ISO 8601 UTC", e);
        }
    }

}
