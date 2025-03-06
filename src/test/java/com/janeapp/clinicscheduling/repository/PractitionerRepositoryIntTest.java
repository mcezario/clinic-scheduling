package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.BaseIntTest;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PractitionerRepositoryIntTest extends BaseIntTest {

    @Autowired
    private PractitionerRepository practitionerRepository;

    @AfterEach
    public void cleanUp() {
        practitionerRepository.deleteAll();
    }

    @Test
    public void shouldCreatePractitionerSuccessfully() {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();

        // When
        Practitioner savedPractitioner = practitionerRepository.save(practitioner);

        // Then
        assertThat(savedPractitioner, not(nullValue()));
        assertThat(savedPractitioner.getId(), not(nullValue()));
        assertThat(savedPractitioner.getCreatedAt(), not(nullValue()));
        assertThat(savedPractitioner.getUpdatedAt(), not(nullValue()));
        assertThat(savedPractitioner.getFirstName(), equalTo(practitioner.getFirstName()));
        assertThat(savedPractitioner.getLastName(), equalTo(practitioner.getLastName()));
        assertThat(savedPractitioner.getEmail(), equalTo(practitioner.getEmail()));
        assertThat(savedPractitioner.getPhone(), equalTo(practitioner.getPhone()));
    }

    @Test
    public void shouldNotAllowDuplicateEmail() {
        // Given
        Practitioner practitioner1 = PractitionerFixture.randomPractitioner();
        Practitioner practitioner2 = PractitionerFixture.randomPractitioner();
        practitioner2.setEmail(practitioner1.getEmail());

        // When
        Practitioner firstAttempt = practitionerRepository.save(practitioner1);
        RuntimeException secondAttempt = assertThrows(RuntimeException.class, () -> practitionerRepository.save(practitioner2));

        // Then
        assertThat(firstAttempt, not(nullValue()));
        assertThat(secondAttempt.getMessage(), containsString("ERROR: duplicate key value violates unique constraint \"practitioner_email_key\""));
        assertThat(secondAttempt, instanceOf(DataIntegrityViolationException.class));
    }

}
