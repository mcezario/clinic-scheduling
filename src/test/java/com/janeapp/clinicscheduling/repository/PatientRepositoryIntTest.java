package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.BaseIntTest;
import com.janeapp.clinicscheduling.entity.Patient;
import com.janeapp.clinicscheduling.entity.fixture.PatientFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PatientRepositoryIntTest extends BaseIntTest {

    @Autowired
    private PatientRepository patientRepository;

    @AfterEach
    public void cleanUp() {
        patientRepository.deleteAll();
    }

    @Test
    public void shouldCreatePatientSuccessfully() {
        // Given
        Patient patient = PatientFixture.randomPatient();

        // When
        Patient savedPatient = patientRepository.save(patient);

        // Then
        assertThat(savedPatient, not(nullValue()));
        assertThat(savedPatient.getId(), not(nullValue()));
        assertThat(savedPatient.getCreatedAt(), not(nullValue()));
        assertThat(savedPatient.getUpdatedAt(), not(nullValue()));
        assertThat(savedPatient.getFirstName(), equalTo(patient.getFirstName()));
        assertThat(savedPatient.getLastName(), equalTo(patient.getLastName()));
        assertThat(savedPatient.getEmail(), equalTo(patient.getEmail()));
        assertThat(savedPatient.getPhone(), equalTo(patient.getPhone()));
        assertThat(savedPatient.getDateOfBirth(), equalTo(patient.getDateOfBirth()));
        assertThat(savedPatient.getStreet(), equalTo(patient.getStreet()));
        assertThat(savedPatient.getCity(), equalTo(patient.getCity()));
        assertThat(savedPatient.getState(), equalTo(patient.getState()));
        assertThat(savedPatient.getPostalCode(), equalTo(patient.getPostalCode()));
        assertThat(savedPatient.getCountry(), equalTo(patient.getCountry()));
    }

    @Test
    public void shouldNotAllowDuplicateEmail() {
        // Given
        Patient patient1 = PatientFixture.randomPatient();
        Patient patient2 = PatientFixture.randomPatient();
        patient2.setEmail(patient1.getEmail());

        // When
        Patient firstAttempt = patientRepository.save(patient1);
        RuntimeException secondAttempt = assertThrows(RuntimeException.class, () -> patientRepository.save(patient2));

        // Then
        assertThat(firstAttempt, not(nullValue()));
        assertThat(secondAttempt.getMessage(), containsString("ERROR: duplicate key value violates unique constraint \"patient_email_key\""));
        assertThat(secondAttempt, instanceOf(DataIntegrityViolationException.class));
    }

}
