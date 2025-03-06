package com.janeapp.clinicscheduling.entity.fixture;

import com.github.javafaker.Faker;
import com.janeapp.clinicscheduling.entity.Practitioner;

import java.util.Locale;

public class PractitionerFixture {

    private static Faker faker = new Faker(new Locale("en-CA"));

    public static Practitioner randomPractitioner() {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();

        Practitioner practitioner = new Practitioner();
        practitioner.setFirstName(firstName);
        practitioner.setLastName(lastName);
        practitioner.setEmail(String.format("%s.%s@gmail.com", firstName.toLowerCase(), lastName.toLowerCase()));
        practitioner.setPhone(faker.phoneNumber().cellPhone());
        return practitioner;
    }

}
