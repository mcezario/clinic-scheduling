package com.janeapp.clinicscheduling.entity.fixture;

import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Patient;
import com.janeapp.clinicscheduling.entity.Practitioner;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class AppointmentFixture {

    public static Appointment weekend(String timezone, int hour) {
        AppointmentType appointmentType = AppointmentType.INITIAL;
        Appointment appointment = new Appointment();
        appointment.setType(appointmentType);
        appointment.setStartAt(getNextWeekend(timezone, hour));
        appointment.setEndAt(appointment.getStartAt().plus(appointmentType.amount(), appointment.getType().unit()));

        return appointment;
    }

    public static Appointment withTimeAndPractitioner(final Instant time, final Practitioner practitioner) {
        Appointment appointment = withTimeAndType(time, AppointmentType.STANDARD);
        appointment.setPractitioner(practitioner);
        return appointment;
    }

    public static Appointment withTimeAndPractitionerAndType(final Instant time, final Practitioner practitioner, AppointmentType appointmentType) {
        Appointment appointment = withTimeAndType(time, appointmentType);
        appointment.setPractitioner(practitioner);
        return appointment;
    }

    public static Appointment withTime(final Instant time) {
        return withTimeAndType(time, AppointmentType.STANDARD);
    }

    public static Appointment withTimeAndType(final Instant time, AppointmentType appointmentType) {
        Appointment appointment = new Appointment();
        appointment.setStartAt(time);
        appointment.setEndAt(appointment.getStartAt().plus(appointmentType.amount(), appointmentType.unit()));
        appointment.setType(appointmentType);

        return appointment;
    }

    public static Appointment withAllDetails(final Instant time, final AppointmentType appointmentType, final Patient patient, final Practitioner practitioner) {
        Appointment appointment = new Appointment();
        appointment.setType(appointmentType);
        appointment.setStartAt(time);
        appointment.setEndAt(time.plus(appointmentType.amount(), appointmentType.unit()));
        appointment.setComment(null);
        appointment.setPatient(patient);
        appointment.setPractitioner(practitioner);

        return appointment;
    }

    private static Instant getNextWeekend(String timezone, int hour) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone)).withHour(hour);
        while (now.getDayOfWeek() != DayOfWeek.SATURDAY && now.getDayOfWeek() != DayOfWeek.SUNDAY) {
            now = now.plusDays(1);
        }
        return now.truncatedTo(ChronoUnit.HOURS).toInstant();
    }
}
