package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.BaseIntTest;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Patient;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.fixture.AppointmentFixture;
import com.janeapp.clinicscheduling.entity.fixture.PatientFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AppointmentRepositoryIntTest extends BaseIntTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @AfterEach
    public void cleanUp() {
        appointmentRepository.deleteAll();
        patientRepository.deleteAll();
        practitionerRepository.deleteAll();
    }

    @Test
    public void shouldCreateAppointmentSuccessfully() {
        // Given
        final Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        final AppointmentType appointmentType = AppointmentType.CHECK_IN;
        final String comment = "Dummy comment";
        final Appointment appointment = createAppointmentDetails(startAt, appointmentType, comment);

        // When
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Then
        assertThat(savedAppointment, not(nullValue()));
        assertThat(savedAppointment.getId(), not(nullValue()));
        assertThat(savedAppointment.getCreatedAt(), not(nullValue()));
        assertThat(savedAppointment.getUpdatedAt(), not(nullValue()));
        assertThat(savedAppointment.getPatient(), equalTo(appointment.getPatient()));
        assertThat(savedAppointment.getPractitioner(), equalTo(appointment.getPractitioner()));
        assertThat(savedAppointment.getStartAt(), equalTo(appointment.getStartAt()));
        assertThat(savedAppointment.getStartAt().atZone(ZoneId.systemDefault()).getSecond(), equalTo(0));
        assertThat(savedAppointment.getEndAt(), equalTo(appointment.getEndAt()));
        assertThat(savedAppointment.getEndAt().atZone(ZoneId.systemDefault()).getSecond(), equalTo(0));
        assertThat(savedAppointment.getType(), equalTo(appointmentType));
        assertThat(savedAppointment.getComment(), equalTo(comment));
    }

    @Test
    public void shouldFindAppointmentForAGivenTimeAndPractitioner() {
        // Given
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        AppointmentType appointmentType = AppointmentType.INITIAL;
        Appointment appointment = createAppointmentDetails(startAt, appointmentType, null);
        appointmentRepository.save(appointment);

        // When
        Instant dateTimeSearch = startAt
                .plus(30, ChronoUnit.MINUTES); // INITIAL appt is 90 min. This should be in the search range
        boolean appointmentFound = appointmentRepository.existsAppointmentInTimeRange(dateTimeSearch, appointment.getPractitioner());

        // Then
        assertThat(appointmentFound, equalTo(true));
    }

    @Test
    public void shouldNotFindAppointmentForAGivenTimeAndPractitioner() {
        // Given
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        AppointmentType appointmentType = AppointmentType.INITIAL;
        Appointment appointment = createAppointmentDetails(startAt, appointmentType, null);
        appointmentRepository.save(appointment);

        // When
        Instant dateTimeSearch = startAt
                .plus(3, ChronoUnit.HOURS); // INITIAL appt is 90 min. This should be out of the search range
        boolean appointmentFound = appointmentRepository.existsAppointmentInTimeRange(dateTimeSearch, appointment.getPractitioner());

        // Then
        assertThat(appointmentFound, equalTo(false));
    }

    @Test
    public void shouldFindFindAppointmentForAGivenTimeRangeAndAppointmentType() {
        // Given
        Instant startAt1 = Instant.now().plus(1, ChronoUnit.DAYS);
        AppointmentType appointmentType1 = AppointmentType.INITIAL;
        Appointment appointment1 = createAppointmentDetails(startAt1, appointmentType1, null);
        appointmentRepository.save(appointment1);

        Instant startAt2 = Instant.now().plus(5, ChronoUnit.DAYS);
        AppointmentType appointmentType2 = AppointmentType.STANDARD;
        Appointment appointment2 = createAppointmentDetails(startAt2, appointmentType2, null);
        Appointment savedAppointment2 = appointmentRepository.save(appointment2);

        // When
        Instant startSearch = startAt2
                .minus(1, ChronoUnit.HOURS);
        Instant endSearch = appointment2.getEndAt()
                .plus(1, ChronoUnit.HOURS);
        List<Appointment> appointments = appointmentRepository
                .findAppointmentsByRangeAndType(appointmentType2.name(), startSearch, endSearch);

        // Then
        assertThat(appointments, hasSize(1));
        assertThat(appointments.get(0).getId(), equalTo(savedAppointment2.getId()));
        assertThat(appointments.get(0).getType(), equalTo(appointmentType2));
        assertThat(appointments.get(0).getStartAt(), equalTo(appointment2.getStartAt()));
        assertThat(appointments.get(0).getEndAt(), equalTo(appointment2.getEndAt()));
    }

    @Test
    public void shouldNotFindAppointmentForAGivenTimeRangeAndAppointmentType() {
        // Given
        Instant startAt1 = Instant.now().plus(1, ChronoUnit.DAYS);
        AppointmentType appointmentType1 = AppointmentType.INITIAL;
        Appointment appointment1 = createAppointmentDetails(startAt1, appointmentType1, null);
        appointmentRepository.save(appointment1);

        Instant startAt2 = Instant.now().plus(5, ChronoUnit.DAYS);
        AppointmentType appointmentType2 = AppointmentType.STANDARD;
        Appointment appointment2 = createAppointmentDetails(startAt2, appointmentType2, null);
        appointmentRepository.save(appointment2);

        // When
        Instant startSearch = startAt2
                .minus(1, ChronoUnit.HOURS);
        Instant endSearch = appointment2.getEndAt()
                .plus(1, ChronoUnit.HOURS);
        List<Appointment> appointments = appointmentRepository
                .findAppointmentsByRangeAndType(appointmentType1.name(), startSearch, endSearch);  // Although the time is within the range, the appt type doesn't match the time frame

        // Then
        assertThat(appointments, hasSize(0));
    }

    @Test
    public void shouldFindFindAppointmentForAGivenPractitionerAndTime() {
        // Given
        Patient patient1 = saveRandomPatient();
        Patient patient2 = saveRandomPatient();
        Practitioner practitioner1 = saveRandomPractitioner();
        Practitioner practitioner2 = saveRandomPractitioner();

        Instant startAt1 = Instant.now().plus(1, ChronoUnit.DAYS);
        Appointment appointment1 = createAppointmentDetails(patient1, practitioner1, startAt1);
        Appointment appointment2 = createAppointmentDetails(patient2, practitioner1, startAt1.plus(2, ChronoUnit.HOURS));
        Appointment appointment3 = createAppointmentDetails(patient2, practitioner2, startAt1);
        appointmentRepository.save(appointment1);
        appointmentRepository.save(appointment2);
        appointmentRepository.save(appointment3);

        // When
        Instant startSearch = appointment2.getEndAt()
                .minus(1, ChronoUnit.HOURS);
        Instant endSearch = startSearch
                .plus(3, ChronoUnit.HOURS);
        List<Appointment> practitioner1Appointments = appointmentRepository.findPractitionerAppointmentsByTimeRange(startSearch, endSearch, practitioner1);

        // Then
        assertThat(practitioner1Appointments, hasSize(1));
        assertThat(practitioner1Appointments.get(0).getId(), equalTo(appointment2.getId()));
        assertThat(practitioner1Appointments.get(0).getType(), equalTo(appointment2.getType()));
        assertThat(practitioner1Appointments.get(0).getStartAt(), equalTo(appointment2.getStartAt()));
        assertThat(practitioner1Appointments.get(0).getEndAt(), equalTo(appointment2.getEndAt()));
    }


    @Test
    public void shouldNotAllowAppointmentCreationForSamePractitionerAndScheduleTime() {
        // Given
        Practitioner practitioner = saveRandomPractitioner();
        Patient patient1 = saveRandomPatient();
        Patient patient2 = saveRandomPatient();
        Instant schedule = Instant.now();
        Appointment appointment1 = createAppointmentDetails(patient1, practitioner, schedule);
        Appointment appointment2 = createAppointmentDetails(patient2, practitioner, schedule);

        // When
        Appointment firstAttempt = appointmentRepository.save(appointment1);
        RuntimeException secondAttempt = assertThrows(RuntimeException.class, () -> appointmentRepository.save(appointment2));

        // Then
        assertThat(firstAttempt, not(nullValue()));
        assertThat(secondAttempt.getMessage(), containsString("ERROR: duplicate key value violates unique constraint \"unique_practitioner_schedule\""));
        assertThat(secondAttempt, instanceOf(DataIntegrityViolationException.class));
    }

    @Test
    public void shouldNotUpdateStaledAppointmentInformation() {
        // Given
        Appointment appointment = createAppointmentDetails(Instant.now(), AppointmentType.INITIAL, null);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // When
        Appointment appointment1 = appointmentRepository.findById(savedAppointment.getId()).get();
        Appointment appointment2 = appointmentRepository.findById(savedAppointment.getId()).get();
        appointment1.setEndAt(appointment1.getStartAt().plus(1, ChronoUnit.HOURS));
        Appointment updatedAppointment1 = appointmentRepository.save(appointment1);

        appointment2.setComment("Updated comment");
        RuntimeException invalidUpdateAttempt = assertThrows(RuntimeException.class, () -> appointmentRepository.save(appointment2));

        // Then
        assertThat(updatedAppointment1.getVersion(), greaterThan(appointment1.getVersion()));
        assertThat(updatedAppointment1.getVersion(), greaterThan(appointment2.getVersion()));
        assertThat(invalidUpdateAttempt.getMessage(), containsString("Row was updated or deleted by another transaction"));
        assertThat(invalidUpdateAttempt, instanceOf(ObjectOptimisticLockingFailureException.class));
    }

    public Appointment createAppointmentDetails(final Patient patient, final Practitioner practitioner, final Instant startAt) {
        AppointmentType appointmentType = AppointmentType.INITIAL;
        Appointment appointment = new Appointment();
        appointment.setType(appointmentType);
        appointment.setStartAt(startAt);
        appointment.setEndAt(startAt.plus(appointmentType.amount(), appointmentType.unit()));
        appointment.setPatient(patient);
        appointment.setPractitioner(practitioner);

        return appointment;
    }

    public Appointment createAppointmentDetails(final Instant startAt, final AppointmentType appointmentType, final String comment) {
        Patient patient = saveRandomPatient();
        Practitioner practitioner = saveRandomPractitioner();

        Appointment appointment = AppointmentFixture.withAllDetails(startAt, appointmentType, patient, practitioner);
        appointment.setComment(comment);

        return appointment;
    }

    public Practitioner saveRandomPractitioner() {
        return practitionerRepository.save(PractitionerFixture.randomPractitioner());
    }

    public Patient saveRandomPatient() {
        return patientRepository.save(PatientFixture.randomPatient());
    }

}
