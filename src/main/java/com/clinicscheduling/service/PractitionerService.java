package com.janeapp.clinicscheduling.service;

import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.repository.AppointmentRepository;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PractitionerService {

    private static final Logger logger = LoggerFactory.getLogger(PractitionerService.class);

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Practitioner> getAllPractitioners() {
        return practitionerRepository.findAll();
    }

    public List<Appointment> getCurrentDayAppointmentsByPractitioner(final Practitioner practitioner) {
        LocalDate now = LocalDate.now();
        ZoneOffset utc = ZoneOffset.UTC;
        Instant start = now.atStartOfDay(utc).toInstant();
        Instant end = now.atTime(LocalTime.MAX).atZone(utc).toInstant();

        List<Appointment> appointments = appointmentRepository.findPractitionerAppointmentsByTimeRange(start, end, practitioner);
        logger.debug("{} records found for practitioner {} with data range {} - {}",
                appointments.size(), practitioner.getId(), start, end);

        return appointments;
    }

}
