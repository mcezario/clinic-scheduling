package com.janeapp.clinicscheduling.controller;

import com.janeapp.clinicscheduling.controller.json.AppointmentPractitionerResponse;
import com.janeapp.clinicscheduling.controller.mapper.AppointmentMapper;
import com.janeapp.clinicscheduling.controller.mapper.PractitionerMapper;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.service.PractitionerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping( "/practitioners" )
public class PractitionerController {

    @Autowired
    private PractitionerService practitionerService;

    @Autowired
    private PractitionerMapper practitionerMapper;

    @Autowired
    private AppointmentMapper mapper;

    @GetMapping( "/{id}/appointments" )
    public List<AppointmentPractitionerResponse> getCurrentDayAppointments(@RequestHeader( value = "Time-Zone", defaultValue = "UTC" ) String timeZone,
                                                                           @PathVariable Long id) {
        List<Appointment> appointments = practitionerService.getCurrentDayAppointmentsByPractitioner(practitionerMapper.fromId(id));
        return mapper.toResponse(appointments, timeZone);
    }

}
