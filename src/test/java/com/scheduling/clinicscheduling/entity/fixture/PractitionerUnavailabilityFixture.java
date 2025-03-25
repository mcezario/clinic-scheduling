package com.janeapp.clinicscheduling.entity.fixture;

import com.github.javafaker.Faker;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.PractitionerUnavailability;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PractitionerUnavailabilityFixture {

    private static Faker faker = new Faker();

    public static PractitionerUnavailability randomTime(final Practitioner practitioner) {
        Instant startTime = Instant.now().plus(faker.random().nextInt(5), ChronoUnit.DAYS);
        Instant endTime = startTime.plus(faker.random().nextInt(5), ChronoUnit.HOURS);
        return withTime(practitioner, startTime, endTime);
    }

    public static PractitionerUnavailability withTime(final Practitioner practitioner, final Instant startTime, final Instant endTime) {
        PractitionerUnavailability unavailability = new PractitionerUnavailability();
        unavailability.setPractitioner(practitioner);
        unavailability.setStartAt(startTime);
        unavailability.setEndAt(endTime);
        unavailability.setReason(faker.lorem().sentence());

        return unavailability;
    }

}
