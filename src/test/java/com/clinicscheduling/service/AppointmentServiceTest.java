package com.janeapp.clinicscheduling.service;

import com.janeapp.clinicscheduling.config.BusinessHoursConfig;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.PractitionerUnavailability;
import com.janeapp.clinicscheduling.entity.fixture.AppointmentFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import com.janeapp.clinicscheduling.repository.AppointmentRepository;
import com.janeapp.clinicscheduling.repository.PractitionerUnavailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith( MockitoExtension.class )
public class AppointmentServiceTest {

    private static final int APPOINTMENT_NOTICE_HOURS = 2;

    private static final String CLINIC_PST_TIMEZONE = "America/Los_Angeles";

    private static final int CLINIC_PST_AMPM_START_HOUR = 9;

    private static final int CLINIC_PST_AMPM_END_HOUR = 17;

    private static final int CLINIC_PST_HH_END_HOUR = 5;

    private static final String CLINIC_NOTICE_HOURS_MSG = "Bookings cannot be made within %d hours of the appointment start time";

    private static final String CLINIC_HOURS_MSG = "Bookings can only be made for appointments that start and end within the clinic hours (%d am to %d pm)";

    private static final String CLINIC_WEEKENDS_MSG = "Bookings cannot be made for weekends";

    private static final String CLINIC_SLOT_TIME_MSG = "Appointments start on the hour or half-hour";

    private static final String APPOINTMENT_OVERLAP_MSG = "The requested time is not available";

    private static final String SPOTS_AVAILABILITY_ERROR_MSG = "No appointments available for the given date";

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PractitionerUnavailabilityRepository practitionerUnavailabilityRepository;

    @Mock
    private BusinessHoursConfig businessHoursConfig;

    @Mock
    private PractitionerService practitionerService;

    @InjectMocks
    private AppointmentService appointmentService;

    @BeforeEach
    public void setUp() {
        when(businessHoursConfig.timezone()).thenReturn(CLINIC_PST_TIMEZONE);
    }

    @Test
    void shouldGetAvailableSpotsWithCompleteAvailability() {
        // Given
        AppointmentType initialAppointmentType = AppointmentType.STANDARD;
        LocalDate date = LocalDate.now(ZoneId.of(CLINIC_PST_TIMEZONE)).plus(2, ChronoUnit.DAYS);
        Practitioner practitioner1 = PractitionerFixture.randomPractitioner();
        practitioner1.setId(1l);
        Practitioner practitioner2 = PractitionerFixture.randomPractitioner();
        practitioner2.setId(2l);
        List<Practitioner> practitioners = List.of(practitioner1, practitioner2);

        // Prepare
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(practitionerService.getAllPractitioners()).thenReturn(practitioners);
        when(appointmentRepository.findAppointmentsByRangeAndType(eq(initialAppointmentType.name()), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(practitionerUnavailabilityRepository.findUnavailabilityForTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // When
        Map<Practitioner, List<TimeSlot>> availableSpots = appointmentService.getAvailableSpotsByTypeAndDate(initialAppointmentType, date);

        // Then
        assertThat(availableSpots.size(), equalTo(practitioners.size()));
        assertThat(availableSpots.get(practitioner1), hasSize(8));
        assertThat(availableSpots.get(practitioner2), hasSize(8));
    }

    @Test
    void shouldGetAvailableSpotsByTypeAndDateConsideringPractitionersAppointments() {
        // Given
        AppointmentType initialAppointmentType = AppointmentType.STANDARD;
        LocalDate date = LocalDate.now(ZoneId.of(CLINIC_PST_TIMEZONE)).plus(2, ChronoUnit.DAYS);
        Practitioner practitioner1 = PractitionerFixture.randomPractitioner();
        practitioner1.setId(1l);
        Practitioner practitioner2 = PractitionerFixture.randomPractitioner();
        practitioner2.setId(2l);
        Practitioner practitioner3 = PractitionerFixture.randomPractitioner();
        practitioner3.setId(3l);
        Instant appointment1Time = date.atTime(CLINIC_PST_AMPM_START_HOUR, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        Appointment appointment1 = AppointmentFixture.withTimeAndPractitionerAndType(appointment1Time, practitioner1, initialAppointmentType);
        Instant appointment2Time = date.atTime(CLINIC_PST_AMPM_START_HOUR + 3, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        Appointment appointment2 = AppointmentFixture.withTimeAndPractitionerAndType(appointment2Time, practitioner3, initialAppointmentType);
        Instant appointment3Time = date.atTime(CLINIC_PST_AMPM_START_HOUR + 4, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        Appointment appointment3 = AppointmentFixture.withTimeAndPractitionerAndType(appointment3Time, practitioner3, initialAppointmentType);
        List<Practitioner> practitioners = List.of(practitioner1, practitioner2, practitioner3);

        // Prepare
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(practitionerService.getAllPractitioners()).thenReturn(practitioners);
        when(appointmentRepository.findAppointmentsByRangeAndType(eq(initialAppointmentType.name()), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(appointment1, appointment2, appointment3));
        when(practitionerUnavailabilityRepository.findUnavailabilityForTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // When
        Map<Practitioner, List<TimeSlot>> availableSpots = appointmentService.getAvailableSpotsByTypeAndDate(initialAppointmentType, date);

        // Then
        assertThat(availableSpots.size(), equalTo(practitioners.size()));
        assertThat(availableSpots.get(practitioner1), hasSize(7));
        assertThat(availableSpots.get(practitioner1), not(hasItem(
                new TimeSlot(appointment1.getStartAt(), appointment1.getEndAt())))
        );
        assertThat(availableSpots.get(practitioner2), hasSize(8));
        assertThat(availableSpots.get(practitioner3), hasSize(6));
        assertThat(availableSpots.get(practitioner3), not(hasItems(
                new TimeSlot(appointment2.getStartAt(), appointment2.getEndAt()),
                new TimeSlot(appointment3.getStartAt(), appointment3.getEndAt())
        )));
    }

    @Test
    void shouldGetAvailableSpotsByTypeAndDateConsideringPractitionersUnavailability() {
        // Given
        AppointmentType initialAppointmentType = AppointmentType.STANDARD;
        LocalDate date = LocalDate.now(ZoneId.of(CLINIC_PST_TIMEZONE)).plus(2, ChronoUnit.DAYS);
        Practitioner practitioner1 = PractitionerFixture.randomPractitioner();
        practitioner1.setId(1l);
        Practitioner practitioner2 = PractitionerFixture.randomPractitioner();
        practitioner2.setId(2l);
        List<Practitioner> practitioners = List.of(practitioner1, practitioner2);

        Instant practitioner1UnavailabilityTime = date.atTime(CLINIC_PST_AMPM_START_HOUR + 3, 30).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        PractitionerUnavailability practitioner1Unavailability = new PractitionerUnavailability();
        practitioner1Unavailability.setPractitioner(practitioner1);
        practitioner1Unavailability.setStartAt(practitioner1UnavailabilityTime);
        practitioner1Unavailability.setEndAt(practitioner1UnavailabilityTime.plus(1, ChronoUnit.HOURS));

        // Prepare
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(practitionerService.getAllPractitioners()).thenReturn(practitioners);
        when(appointmentRepository.findAppointmentsByRangeAndType(eq(initialAppointmentType.name()), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        when(practitionerUnavailabilityRepository.findUnavailabilityForTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(practitioner1Unavailability));

        // When
        Map<Practitioner, List<TimeSlot>> availableSpots = appointmentService.getAvailableSpotsByTypeAndDate(initialAppointmentType, date);

        // Then
        assertThat(availableSpots.size(), equalTo(practitioners.size()));
        assertThat(availableSpots.get(practitioner1), hasSize(6));
        assertThat(availableSpots.get(practitioner1), not(hasItems(
                // The practitioner is unavailability for 30 min and given that the appointment is Standard, this will block 2 lots
                new TimeSlot(practitioner1Unavailability.getStartAt(), practitioner1Unavailability.getEndAt()),
                new TimeSlot(practitioner1Unavailability.getStartAt().plus(1, ChronoUnit.HOURS), practitioner1Unavailability.getEndAt().plus(1, ChronoUnit.HOURS))
        )));
        assertThat(availableSpots.get(practitioner2), hasSize(8));
    }

    @Test
    void shouldNotReturnAvailabilityForPastDate() {
        // Given
        AppointmentType appointmentType = mock(AppointmentType.class);
        LocalDate date = LocalDate.now(ZoneId.of(CLINIC_PST_TIMEZONE)).minus(2, ChronoUnit.DAYS);

        // Prepare
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.getAvailableSpotsByTypeAndDate(appointmentType, date));

        // Then
        assertThat(exception.getMessage(), equalTo(SPOTS_AVAILABILITY_ERROR_MSG));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).findAppointmentsByRangeAndType(any(), any(), any());
        verify(practitionerUnavailabilityRepository, never()).findUnavailabilityForTimeRange(any(), any());
        verify(practitionerService, never()).getAllPractitioners();
    }

    @Test
    void shouldNotReturnAvailabilityWhenTimeIsOutOfBusinessNoticeRange() {
        // Given
        AppointmentType appointmentType = mock(AppointmentType.class);
        LocalDate date = LocalDate.now(ZoneId.of(CLINIC_PST_TIMEZONE));

        // Prepare
        when(businessHoursConfig.appointmentNoticeHours()).thenReturn(APPOINTMENT_NOTICE_HOURS);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        ZonedDateTime zdt = ZonedDateTime.of(date, LocalTime.of(CLINIC_PST_AMPM_END_HOUR - APPOINTMENT_NOTICE_HOURS, 0), ZoneId.of(CLINIC_PST_TIMEZONE));
        ZonedDateTime zdtStart = zdt.plus(businessHoursConfig.appointmentNoticeHours(), ChronoUnit.HOURS);
        ZonedDateTime zdtEnd = ZonedDateTime.of(date, LocalTime.of(businessHoursConfig.ampmEnd(), 0), ZoneId.of(CLINIC_PST_TIMEZONE));
        ZoneId zoneId = ZoneId.of(CLINIC_PST_TIMEZONE);

        try (MockedStatic<ZonedDateTime> mockedStatic = mockStatic(ZonedDateTime.class)) {
            mockedStatic.when(() -> ZonedDateTime.now(zoneId)).thenReturn(zdt);
            mockedStatic.when(() -> zdt.plus(businessHoursConfig.appointmentNoticeHours(), ChronoUnit.HOURS)).thenReturn(zdtStart);
            mockedStatic.when(() -> zdt.withHour(businessHoursConfig.ampmEnd())).thenReturn(zdtEnd);

            // When
            RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.getAvailableSpotsByTypeAndDate(appointmentType, date));

            // Then
            assertThat(exception.getMessage(), equalTo(SPOTS_AVAILABILITY_ERROR_MSG));
            assertThat(exception, instanceOf(AppointmentValidationException.class));
            verify(appointmentRepository, never()).findAppointmentsByRangeAndType(any(), any(), any());
            verify(practitionerUnavailabilityRepository, never()).findUnavailabilityForTimeRange(any(), any());
            verify(practitionerService, never()).getAllPractitioners();
        }
    }

    @Test
    void shouldGetAvailableSpotsByTypeAndDateConsideringPractitionersUnavailabilityAndAppointments() {
        // Given
        AppointmentType initialAppointmentType = AppointmentType.STANDARD;
        LocalDate date = LocalDate.now(ZoneId.of(CLINIC_PST_TIMEZONE)).plus(2, ChronoUnit.DAYS);
        Practitioner practitioner1 = PractitionerFixture.randomPractitioner();
        practitioner1.setId(1l);
        Practitioner practitioner2 = PractitionerFixture.randomPractitioner();
        practitioner2.setId(2l);
        Practitioner practitioner3 = PractitionerFixture.randomPractitioner();
        practitioner3.setId(3l);
        Instant appointment1Time = date.atTime(CLINIC_PST_AMPM_START_HOUR, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        Appointment appointment1 = AppointmentFixture.withTimeAndPractitionerAndType(appointment1Time, practitioner1, initialAppointmentType);
        Instant appointment2Time = date.atTime(CLINIC_PST_AMPM_START_HOUR + 3, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        Appointment appointment2 = AppointmentFixture.withTimeAndPractitionerAndType(appointment2Time, practitioner3, initialAppointmentType);
        Instant appointment3Time = date.atTime(CLINIC_PST_AMPM_START_HOUR + 4, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        Appointment appointment3 = AppointmentFixture.withTimeAndPractitionerAndType(appointment3Time, practitioner3, initialAppointmentType);
        List<Practitioner> practitioners = List.of(practitioner1, practitioner2, practitioner3);

        Instant practitioner3UnavailabilityTime = date.atTime(CLINIC_PST_AMPM_START_HOUR + 5, 0).atZone(ZoneId.of(CLINIC_PST_TIMEZONE)).toInstant();
        PractitionerUnavailability practitioner3Unavailability = new PractitionerUnavailability();
        practitioner3Unavailability.setPractitioner(practitioner3);
        practitioner3Unavailability.setStartAt(practitioner3UnavailabilityTime);
        practitioner3Unavailability.setEndAt(practitioner3UnavailabilityTime.plus(1, ChronoUnit.HOURS));

        // Prepare
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(practitionerService.getAllPractitioners()).thenReturn(practitioners);
        when(appointmentRepository.findAppointmentsByRangeAndType(eq(initialAppointmentType.name()), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(appointment1, appointment2, appointment3));
        when(practitionerUnavailabilityRepository.findUnavailabilityForTimeRange(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // When
        Map<Practitioner, List<TimeSlot>> availableSpots = appointmentService.getAvailableSpotsByTypeAndDate(initialAppointmentType, date);

        // Then
        assertThat(availableSpots.size(), equalTo(practitioners.size()));
        assertThat(availableSpots.get(practitioner1), hasSize(7));
        assertThat(availableSpots.get(practitioner1), not(hasItem(
                new TimeSlot(appointment1.getStartAt(), appointment1.getEndAt())))
        );
        assertThat(availableSpots.get(practitioner2), hasSize(8));
        assertThat(availableSpots.get(practitioner3), hasSize(6));
        assertThat(availableSpots.get(practitioner3), not(hasItems(
                new TimeSlot(appointment2.getStartAt(), appointment2.getEndAt()),
                new TimeSlot(appointment3.getStartAt(), appointment3.getEndAt()),
                new TimeSlot(practitioner3Unavailability.getStartAt(), practitioner3Unavailability.getEndAt())
        )));
    }

    @Test
    void shouldCreateAppointmentSuccessfully() {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Appointment appointment = AppointmentFixture.withTimeAndPractitioner(createValidStartTime(), practitioner);

        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(appointmentRepository.existsAppointmentInTimeRange(appointment.getStartAt(), practitioner)).thenReturn(false);
        when(practitionerUnavailabilityRepository.existsUnavailabilityForTimeRange(appointment.getStartAt(), practitioner)).thenReturn(false);
        Appointment mockAppointment = mock(Appointment.class);
        when(appointmentRepository.save(appointment)).thenReturn(mockAppointment);

        // When
        Appointment savedAppointment = appointmentService.addAppointment(appointment);

        // Then
        assertThat(savedAppointment, not(nullValue()));
        assertThat(savedAppointment, equalTo(mockAppointment));
        verify(appointmentRepository, times(1)).existsAppointmentInTimeRange(appointment.getStartAt(), practitioner);
        verify(practitionerUnavailabilityRepository, times(1)).existsUnavailabilityForTimeRange(appointment.getStartAt(), practitioner);
        verify(appointmentRepository, times(1)).save(appointment);
    }

    @Test
    void shouldValidateAppointmentOverlap() {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Appointment appointment = AppointmentFixture.withTimeAndPractitioner(createValidStartTime(), practitioner);

        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(appointmentRepository.existsAppointmentInTimeRange(appointment.getStartAt(), practitioner)).thenReturn(true);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(APPOINTMENT_OVERLAP_MSG));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, times(1)).existsAppointmentInTimeRange(appointment.getStartAt(), practitioner);
        verify(practitionerUnavailabilityRepository, never()).existsUnavailabilityForTimeRange(appointment.getStartAt(), practitioner);
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void shouldValidatePractitionerUnavailability() {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Appointment appointment = AppointmentFixture.withTimeAndPractitioner(createValidStartTime(), practitioner);

        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(appointmentRepository.existsAppointmentInTimeRange(appointment.getStartAt(), practitioner)).thenReturn(false);
        when(practitionerUnavailabilityRepository.existsUnavailabilityForTimeRange(appointment.getStartAt(), practitioner)).thenReturn(true);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(APPOINTMENT_OVERLAP_MSG));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, times(1)).existsAppointmentInTimeRange(appointment.getStartAt(), practitioner);
        verify(practitionerUnavailabilityRepository, times(1)).existsUnavailabilityForTimeRange(appointment.getStartAt(), practitioner);
        verify(appointmentRepository, never()).save(appointment);
    }

    @Test
    void shouldValidateScheduleOutOfTheAppointmentStartTimeRange() {
        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.appointmentNoticeHours()).thenReturn(APPOINTMENT_NOTICE_HOURS);

        // Given
        Instant time = Instant.now().plus(APPOINTMENT_NOTICE_HOURS, ChronoUnit.HOURS).minus(20, ChronoUnit.MINUTES);
        Appointment appointment = AppointmentFixture.withTime(time);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(String.format(CLINIC_NOTICE_HOURS_MSG, APPOINTMENT_NOTICE_HOURS)));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).save(appointment);
        verify(appointmentRepository, never()).existsAppointmentInTimeRange(any(), any());
    }

    @Test
    void shouldValidateScheduleOnWeekends() {
        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(false);

        // Given
        Appointment appointment = AppointmentFixture.weekend(CLINIC_PST_TIMEZONE, CLINIC_PST_AMPM_START_HOUR + APPOINTMENT_NOTICE_HOURS);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(CLINIC_WEEKENDS_MSG));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).save(appointment);
        verify(appointmentRepository, never()).existsAppointmentInTimeRange(any(), any());
    }

    @Test
    void shouldValidateScheduleTimeWhenClinicIsNotOpen() {
        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.hhEnd()).thenReturn(CLINIC_PST_HH_END_HOUR);

        // Given
        Instant scheduleTimeBeforeClinicIsOpen = ZonedDateTime.now(ZoneId.of(CLINIC_PST_TIMEZONE))
                .withHour(CLINIC_PST_AMPM_START_HOUR - 1)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plus(1, ChronoUnit.DAYS)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
        Appointment appointment = AppointmentFixture.withTime(scheduleTimeBeforeClinicIsOpen);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(String.format(CLINIC_HOURS_MSG, CLINIC_PST_AMPM_START_HOUR, CLINIC_PST_HH_END_HOUR)));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).save(appointment);
        verify(appointmentRepository, never()).existsAppointmentInTimeRange(any(), any());
    }

    @Test
    void shouldValidateScheduleTimeWhenClinicIsClosed() {
        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.hhEnd()).thenReturn(CLINIC_PST_HH_END_HOUR);

        // Given
        Instant scheduleTimeBeforeClinicIsClosed = ZonedDateTime.now(ZoneId.of(CLINIC_PST_TIMEZONE))
                .withHour(CLINIC_PST_HH_END_HOUR + 3)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .plus(1, ChronoUnit.DAYS)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
        Appointment appointment = AppointmentFixture.withTime(scheduleTimeBeforeClinicIsClosed);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(String.format(CLINIC_HOURS_MSG, CLINIC_PST_AMPM_START_HOUR, CLINIC_PST_HH_END_HOUR)));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).save(appointment);
        verify(appointmentRepository, never()).existsAppointmentInTimeRange(any(), any());
    }

    @Test
    void shouldValidateScheduleTimeWhenClinicIsOpenButAppointTypeDoesNotAccommodateTimeRange() {
        // Prepare
        when(businessHoursConfig.weekends()).thenReturn(true);
        when(businessHoursConfig.ampmStart()).thenReturn(CLINIC_PST_AMPM_START_HOUR);
        when(businessHoursConfig.ampmEnd()).thenReturn(CLINIC_PST_AMPM_END_HOUR);
        when(businessHoursConfig.hhEnd()).thenReturn(CLINIC_PST_HH_END_HOUR);

        // Given
        AppointmentType type = AppointmentType.INITIAL;
        Instant scheduleTime = ZonedDateTime.now(ZoneId.of(CLINIC_PST_TIMEZONE))
                .plus(1, ChronoUnit.DAYS)
                .withHour(CLINIC_PST_AMPM_END_HOUR)
                .truncatedTo(ChronoUnit.HOURS) // if today is 2025-01-01, this will return 2025-01-02 05:00pm PST
                .minus(type.amount(), type.unit())  // subtracts the appt time from previous value, this will return 2025-01-02 03:30pm PST (INITIAL appt is 90 minutes).
                .plus(30, ChronoUnit.MINUTES) // adds 30 min, this will return 2025-01-02 04:00pm PST, which is within the hours range but exceeds the clinic hours because INITIAL appt is 90 minutes
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
        Appointment appointment = AppointmentFixture.withTimeAndType(scheduleTime, type);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(String.format(CLINIC_HOURS_MSG, CLINIC_PST_AMPM_START_HOUR, CLINIC_PST_HH_END_HOUR)));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).save(appointment);
        verify(appointmentRepository, never()).existsAppointmentInTimeRange(any(), any());
    }

    @Test
    void shouldValidateScheduleSlotBreakConstraint() {
        // Given
        AppointmentType type = AppointmentType.INITIAL;
        Instant scheduleTime = ZonedDateTime.now(ZoneId.of(CLINIC_PST_TIMEZONE))
                .plus(1, ChronoUnit.DAYS)
                .withHour(CLINIC_PST_AMPM_START_HOUR + 1)
                .withMinute(14)
                .truncatedTo(ChronoUnit.SECONDS) // if today is 2025-01-01, this will return 2025-01-02 10:14 am PST
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
        Appointment appointment = AppointmentFixture.withTimeAndType(scheduleTime, type);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class, () -> appointmentService.addAppointment(appointment));

        // Then
        assertThat(exception.getMessage(), equalTo(String.format(CLINIC_SLOT_TIME_MSG, CLINIC_PST_AMPM_START_HOUR, CLINIC_PST_HH_END_HOUR)));
        assertThat(exception, instanceOf(AppointmentValidationException.class));
        verify(appointmentRepository, never()).save(appointment);
        verify(appointmentRepository, never()).existsAppointmentInTimeRange(any(), any());
    }

    private Instant createValidStartTime() {
        return ZonedDateTime.now(ZoneId.of(CLINIC_PST_TIMEZONE))
                .withHour(CLINIC_PST_AMPM_START_HOUR + 2)
                .plus(2, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.HOURS)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant();
    }

}
