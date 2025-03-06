package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.BaseIntTest;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.PractitionerUnavailability;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerUnavailabilityFixture;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PractitionerUnavailabilityRepositoryIntTest extends BaseIntTest {

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private PractitionerUnavailabilityRepository practitionerUnavailabilityRepository;

    @AfterEach
    public void cleanUp() {
        practitionerUnavailabilityRepository.deleteAll();
    }

    @Test
    public void shouldCreateUnavailabilityForPractitionerSuccessfully() {
        // Given
        Practitioner practitioner = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability = PractitionerUnavailabilityFixture.randomTime(practitioner);

        // When
        PractitionerUnavailability savedUnavailability = practitionerUnavailabilityRepository.save(unavailability);

        // Then
        assertThat(savedUnavailability, not(nullValue()));
        assertThat(savedUnavailability.getId(), not(nullValue()));
        assertThat(savedUnavailability.getCreatedAt(), not(nullValue()));
        assertThat(savedUnavailability.getUpdatedAt(), not(nullValue()));
        assertThat(savedUnavailability.getStartAt(), equalTo(unavailability.getStartAt()));
        assertThat(savedUnavailability.getEndAt(), equalTo(unavailability.getEndAt()));
        assertThat(savedUnavailability.getReason(), equalTo(unavailability.getReason()));
    }

    @Test
    public void shouldNotAllowDuplicateUnavailabilityPerPractitioner() {
        // Given
        Instant startTime = Instant.now().plus(10, ChronoUnit.MINUTES);
        Instant endTime = startTime.plus(3, ChronoUnit.HOURS);
        Practitioner practitioner = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability1 = PractitionerUnavailabilityFixture.withTime(practitioner, startTime, endTime);
        PractitionerUnavailability unavailability2 = PractitionerUnavailabilityFixture.withTime(practitioner, startTime, endTime);

        // When
        PractitionerUnavailability firstAttempt = practitionerUnavailabilityRepository.save(unavailability1);
        RuntimeException secondAttempt = assertThrows(RuntimeException.class, () -> practitionerUnavailabilityRepository.save(unavailability2));

        // Then
        assertThat(firstAttempt, not(nullValue()));
        assertThat(secondAttempt.getMessage(), containsString("ERROR: duplicate key value violates unique constraint \"unique_practitioner_unavailability\""));
        assertThat(secondAttempt.getCause(), instanceOf(ConstraintViolationException.class));
    }

    @Test
    public void shouldFindUnavailabilityForAGivenTimeAndPractitioner() {
        // Given
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(1, ChronoUnit.HOURS);
        Practitioner practitioner = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability = PractitionerUnavailabilityFixture.withTime(practitioner, startAt, endAt);
        practitionerUnavailabilityRepository.save(unavailability);

        // When
        Instant dateTimeSearch = startAt
                .plus(30, ChronoUnit.MINUTES); // This should be in the range startAt <> endAt
        boolean unavailabilityFound = practitionerUnavailabilityRepository.existsUnavailabilityForTimeRange(dateTimeSearch, practitioner);

        // Then
        assertThat(unavailabilityFound, equalTo(true));
    }

    @Test
    public void shouldNotFindUnavailabilityForAGivenTimeAndPractitioner() {
        // Given
        Instant startAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt = startAt.plus(1, ChronoUnit.HOURS);
        Practitioner practitioner = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability = PractitionerUnavailabilityFixture.withTime(practitioner, startAt, endAt);
        practitionerUnavailabilityRepository.save(unavailability);

        // When
        Instant dateTimeSearch = startAt
                .plus(3, ChronoUnit.HOURS); // This should be in the range startAt <> endAt
        boolean unavailabilityFound = practitionerUnavailabilityRepository.existsUnavailabilityForTimeRange(dateTimeSearch, practitioner);

        // Then
        assertThat(unavailabilityFound, equalTo(false));
    }


    @Test
    public void shouldFindUnavailabilityForAGivenTime() {
        // Given
        Instant startAt1 = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt1 = startAt1.plus(1, ChronoUnit.HOURS);
        Practitioner practitioner1 = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability1 = PractitionerUnavailabilityFixture.withTime(practitioner1, startAt1, endAt1);
        practitionerUnavailabilityRepository.save(unavailability1);

        Instant startAt2 = endAt1.plus(2, ChronoUnit.HOURS);
        Instant endAt2 = startAt2.plus(30, ChronoUnit.MINUTES);
        Practitioner practitioner2 = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability2 = PractitionerUnavailabilityFixture.withTime(practitioner2, startAt2, endAt2);
        PractitionerUnavailability savedUnavailability2 = practitionerUnavailabilityRepository.save(unavailability2);

        // When
        Instant startSearch = endAt2
                .minus(30, ChronoUnit.MINUTES); // This should be in the unavailability
        Instant endSearch = startSearch
                .plus(2, ChronoUnit.HOURS);
        List<PractitionerUnavailability> unavailabilityList = practitionerUnavailabilityRepository.findUnavailabilityForTimeRange(startSearch, endSearch);

        // Then
        assertThat(unavailabilityList, hasSize(1));
        assertThat(unavailabilityList.get(0).getId(), equalTo(savedUnavailability2.getId()));
        assertThat(unavailabilityList.get(0).getPractitioner().getId(), equalTo(practitioner2.getId()));
        assertThat(unavailabilityList.get(0).getStartAt(), equalTo(startAt2));
        assertThat(unavailabilityList.get(0).getEndAt(), equalTo(endAt2));
    }

    @Test
    public void shouldNotFindUnavailabilityForAGivenTimeRange() {
        // Given
        Instant startAt1 = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endAt1 = startAt1.plus(1, ChronoUnit.HOURS);
        Practitioner practitioner1 = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability1 = PractitionerUnavailabilityFixture.withTime(practitioner1, startAt1, endAt1);
        practitionerUnavailabilityRepository.save(unavailability1);

        Instant startAt2 = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant endAt2 = startAt2.plus(30, ChronoUnit.MINUTES);
        Practitioner practitioner2 = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        PractitionerUnavailability unavailability2 = PractitionerUnavailabilityFixture.withTime(practitioner2, startAt2, endAt2);
        practitionerUnavailabilityRepository.save(unavailability2);

        // When
        Instant startSearch = endAt2
                .plus(3, ChronoUnit.HOURS); // This should be out of the unavailability
        Instant endSearch = startSearch
                .plus(1, ChronoUnit.HOURS);
        List<PractitionerUnavailability> unavailabilityList = practitionerUnavailabilityRepository.findUnavailabilityForTimeRange(startSearch, endSearch);

        // Then
        assertThat(unavailabilityList, hasSize(0));
    }

}
