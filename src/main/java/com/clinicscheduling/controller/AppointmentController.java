package com.janeapp.clinicscheduling.controller;

import com.janeapp.clinicscheduling.controller.json.AppointmentAvailabilityResponse;
import com.janeapp.clinicscheduling.controller.json.AppointmentRequest;
import com.janeapp.clinicscheduling.controller.json.AppointmentResponse;
import com.janeapp.clinicscheduling.controller.mapper.AppointmentMapper;
import com.janeapp.clinicscheduling.controller.mapper.AppointmentTypeMapper;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.service.AppointmentService;
import com.janeapp.clinicscheduling.service.TimeSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping( "/appointments" )
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private AppointmentTypeMapper appointmentTypeMapper;

    @GetMapping
    public List<AppointmentAvailabilityResponse> getAppointments(
            @RequestHeader( value = "Time-Zone", defaultValue = "UTC" ) String timeZone,
            @RequestParam( "type" ) String appointmentType,
            @RequestParam @DateTimeFormat( iso = DateTimeFormat.ISO.DATE ) LocalDate date) {
        Map<Practitioner, List<TimeSlot>> availableSpotsByTypeAndDate = appointmentService
                .getAvailableSpotsByTypeAndDate(appointmentTypeMapper.fromString(appointmentType), date);
        return appointmentMapper.toResponse(availableSpotsByTypeAndDate, timeZone);
    }

    @PostMapping
    public AppointmentResponse addAppointment(@RequestHeader( value = "Time-Zone", defaultValue = "UTC" ) String timeZone,
                                              @RequestBody AppointmentRequest request) {
        Appointment appointment = appointmentMapper.fromRequest(request, timeZone);
        return appointmentMapper.toResponse(
                appointmentService.addAppointment(appointment)
        );
    }

}
