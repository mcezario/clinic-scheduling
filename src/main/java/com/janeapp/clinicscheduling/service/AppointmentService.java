package com.janeapp.clinicscheduling.service;

import com.janeapp.clinicscheduling.config.BusinessHoursConfig;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.PractitionerUnavailability;
import com.janeapp.clinicscheduling.repository.AppointmentRepository;
import com.janeapp.clinicscheduling.repository.PractitionerUnavailabilityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger logger = LoggerFactory.getLogger(AppointmentService.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PractitionerUnavailabilityRepository practitionerUnavailabilityRepository;

    @Autowired
    private BusinessHoursConfig config;

    @Autowired
    private PractitionerService practitionerService;

    public Map<Practitioner, List<TimeSlot>> getAvailableSpotsByTypeAndDate(final AppointmentType appointmentType, final LocalDate date) {
        ZoneId clinicZoneId = ZoneId.of(config.timezone());
        ZonedDateTime clinicZonedDateTime = ZonedDateTime.now(ZoneId.of(config.timezone()));
        Instant startSearch = clinicZonedDateTime.toLocalDate().equals(date)
                ? clinicZonedDateTime.plus(config.appointmentNoticeHours(), ChronoUnit.HOURS).toInstant()
                : date.atTime(config.ampmStart(), 0).atZone(clinicZoneId).toInstant();
        Instant endSearch = clinicZonedDateTime.toLocalDate().equals(date)
                ? clinicZonedDateTime.withHour(config.ampmEnd()).toInstant()
                : date.atTime(config.ampmEnd(), 0).atZone(clinicZoneId).toInstant();

        if (startSearch.equals(endSearch) || isPastDate(startSearch, clinicZoneId)) {
            logger.error("Date specified is out of range. Input date: {} and type: {}, " +
                    "Search star and end range: {} - {},  Id: {}", date, appointmentType, startSearch, endSearch);
            throw new AppointmentValidationException("No appointments available for the given date");
        }

        Map<Practitioner, List<Appointment>> practitionersAppointments = getAppointmentsSlots(appointmentType, startSearch, endSearch);
        Map<Practitioner, List<PractitionerUnavailability>> practitionersUnavailability = getPractitionersUnavailability(startSearch, endSearch);
        Map<Practitioner, List<TimeSlot>> practitionerSlots = new HashMap<>();
        for (Practitioner practitioner : practitionerService.getAllPractitioners()) {
            List<TimeSlot> unavailableSlots = new ArrayList<>();
            unavailableSlots.addAll(transformPractitionersAppointmentsInTimeSlot(practitionersAppointments, practitioner));
            unavailableSlots.addAll(transformPractitionersUnavailabilityInTimeSlot(practitionersUnavailability, practitioner));

            List<TimeSlot> availableSlots = generateTimeSlots(startSearch, endSearch, unavailableSlots, appointmentType);
            practitionerSlots.put(practitioner, availableSlots);
        }

        logger.debug("{} records found for date {} and type {}", practitionerSlots.size(), date, appointmentType);
        return practitionerSlots;
    }

    private boolean isPastDate(final Instant start, final ZoneId clinicZoneId) {
        LocalDate localDateStart = start.atZone(clinicZoneId).toLocalDate();
        LocalDate localDateNow = Instant.now().atZone(clinicZoneId).toLocalDate();
        return localDateStart.isBefore(localDateNow);
    }

    private List<TimeSlot> transformPractitionersUnavailabilityInTimeSlot(final Map<Practitioner, List<PractitionerUnavailability>> practitionersUnavailability,
                                                                          final Practitioner practitioner) {
        return practitionersUnavailability.getOrDefault(practitioner, List.of())
                .stream().map(pu -> new TimeSlot(pu.getStartAt(), pu.getEndAt()))
                .collect(Collectors.toList());
    }

    private List<TimeSlot> transformPractitionersAppointmentsInTimeSlot(final Map<Practitioner, List<Appointment>> practitionersAppointments,
                                                                        final Practitioner practitioner) {
        return practitionersAppointments.getOrDefault(practitioner, List.of())
                .stream().map(appointment -> new TimeSlot(appointment.getStartAt(), appointment.getEndAt()))
                .collect(Collectors.toList());
    }

    private Map<Practitioner, List<Appointment>> getAppointmentsSlots(final AppointmentType appointmentType, final Instant start,
                                                                      final Instant end) {
        return appointmentRepository
                .findAppointmentsByRangeAndType(appointmentType.name(), start, end)
                .stream()
                .collect(Collectors.groupingBy(Appointment::getPractitioner));
    }

    private Map<Practitioner, List<PractitionerUnavailability>> getPractitionersUnavailability(final Instant start,
                                                                                               final Instant end) {
        return practitionerUnavailabilityRepository
                .findUnavailabilityForTimeRange(start, end).stream()
                .collect(Collectors.groupingBy(PractitionerUnavailability::getPractitioner));
    }

    private List<TimeSlot> generateTimeSlots(final Instant start, final Instant end, List<TimeSlot> unavailableSlots,
                                             final AppointmentType appointmentType) {
        List<TimeSlot> slots = new ArrayList<>();
        Instant incrementedStart = roundMinutes(start);
        Instant newEnd = roundMinutes(end);
        while (! (incrementedStart.equals(newEnd) || incrementedStart.isAfter(newEnd))) {
            Instant nextSlot = incrementedStart.plus(appointmentType.amount(), appointmentType.unit());
            TimeSlot currentSlot = new TimeSlot(incrementedStart, nextSlot);

            boolean isUnavailable = unavailableSlots.stream().anyMatch(slot -> slot.overlapsWith(currentSlot));
            if (! isUnavailable) {
                slots.add(new TimeSlot(incrementedStart, nextSlot));
            }
            incrementedStart = nextSlot;
        }
        return slots;
    }

    /**
     * Adds an appointment based on the given appointment details informed.
     * <p>
     * The method will validate the {@link Appointment#startAt} and {@link Appointment#endAt} against Clinic Hours constraints.
     * Make sure to inform it in UTC date. See below the rules:
     * <ul>
     *     <li>The schedule time should be within the clinic business hours.</li>
     *     <li>Appointments do not overlap. There can only be one booked appointment at any time for a given {@link Appointment#practitioner}.</li>
     *     <li>Appointments start on the hour or half-hour.</li>
     *     <li>Bookings can only be made for appointments that start and end within the clinic hours.</li>
     *     <li>Bookings cannot be made within a configured number of hours of the appointment start time.</li>
     * </ul>
     *
     * @param appointment the appointment details.
     *                    Make sure to inform {@link Appointment#startAt} and {@link Appointment#endAt} in UTC date.
     * @return the stored appointment.
     * @throws AppointmentValidationException if any validation fails
     * @see BusinessHoursConfig for details regarding configuration
     */
    @Transactional
    public Appointment addAppointment(final Appointment appointment) {
        validateScheduleTime(appointment.getStartAt(), appointment.getEndAt());
        try {
            validatePractitionerAvailability(appointment.getStartAt(), appointment.getPractitioner());

            Appointment saveAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment created successfully. Id: {}", saveAppointment.getId());
            return saveAppointment;

        } catch (DataIntegrityViolationException e) {
            throw new AppointmentValidationException("The requested time is not available", e);
        }
    }

    private void validatePractitionerAvailability(final Instant requestedStartTime, final Practitioner practitioner) {
        if (appointmentRepository.existsAppointmentInTimeRange(requestedStartTime, practitioner) ||
                practitionerUnavailabilityRepository.existsUnavailabilityForTimeRange(requestedStartTime, practitioner)) {
            throw new DataIntegrityViolationException(String.format("Practitioner %s not available on %s", practitioner.fullName(), requestedStartTime));
        }
    }

    private Instant roundMinutes(Instant time) {
        ZoneOffset utc = ZoneOffset.UTC;
        LocalDateTime dateTime = time.atZone(utc).toLocalDateTime();
        int newMinutes = dateTime.getMinute() < 30 ? 0 : 30;

        LocalDateTime roundedDateTime = dateTime
                .withMinute(newMinutes)
                .withSecond(0)
                .withNano(0);

        return roundedDateTime.atZone(utc).toInstant();
    }

    private void validateScheduleTime(final Instant requestedStartTime, final Instant requestedEndTime) {
        ZoneId clinicZoneId = ZoneId.of(config.timezone());
        ZonedDateTime requestedStartTimeZoned = requestedStartTime.atZone(clinicZoneId);
        ZonedDateTime requestedEndTimeZoned = requestedEndTime.atZone(clinicZoneId);

        DayOfWeek dayOfWeek = requestedStartTimeZoned.getDayOfWeek();
        if (! config.weekends() && (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)) {
            throw new AppointmentValidationException("Bookings cannot be made for weekends");
        }

        Instant now = Instant.now()
                .plus(5, ChronoUnit.SECONDS); // Extend 5 seconds to avoid edge cases for when now is only milliseconds behind the time range
        if (Duration.between(now, requestedStartTime).toHours() < config.appointmentNoticeHours()) {
            throw new AppointmentValidationException(String.format(
                    "Bookings cannot be made within %d hours of the appointment start time", config.appointmentNoticeHours()
            ));
        }

        int minute = requestedStartTimeZoned.getMinute();
        if (minute != 0 && minute != 30) {
            throw new AppointmentValidationException("Appointments start on the hour or half-hour");
        }

        if (isStartTimeOutOfRange(clinicZoneId, requestedStartTimeZoned) || isEndTimeOutOfRange(clinicZoneId, requestedEndTimeZoned)) {
            throw new AppointmentValidationException(String.format(
                    "Bookings can only be made for appointments that start and end within the clinic hours (%d am to %d pm)",
                    config.ampmStart(), config.hhEnd()
            ));
        }
    }

    private boolean isStartTimeOutOfRange(final ZoneId clinicZoneId, final ZonedDateTime requestedStartTime) {
        ZonedDateTime clinicTimeZoneNow = ZonedDateTime.now(clinicZoneId);
        LocalTime appointmentStartTime = extractTimeFromDate(requestedStartTime);
        LocalTime startTimeConstraint = extractTimeFromDate(
                clinicTimeZoneNow.withHour(config.ampmStart()).truncatedTo(ChronoUnit.HOURS)
        );
        return appointmentStartTime.isBefore(startTimeConstraint);
    }

    private boolean isEndTimeOutOfRange(final ZoneId clinicZoneId, final ZonedDateTime requestedEndTimeZoned) {
        ZonedDateTime clinicTimeZoneNow = ZonedDateTime.now(clinicZoneId);
        LocalTime appointmentEndTime = extractTimeFromDate(requestedEndTimeZoned);
        LocalTime endTimeConstraint = extractTimeFromDate(
                clinicTimeZoneNow.withHour(config.ampmEnd()).truncatedTo(ChronoUnit.HOURS)
        );
        return appointmentEndTime.isAfter(endTimeConstraint);
    }

    private LocalTime extractTimeFromDate(final ZonedDateTime dateTime) {
        return dateTime.toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

}
