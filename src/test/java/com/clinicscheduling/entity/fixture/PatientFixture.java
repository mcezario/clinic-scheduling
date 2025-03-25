package com.janeapp.clinicscheduling.entity.fixture;

import com.github.javafaker.Faker;
import com.janeapp.clinicscheduling.entity.Patient;

import java.time.ZoneId;
import java.util.Locale;

public class PatientFixture {

    private static Faker faker = new Faker(new Locale("en-CA"));

    public static Patient randomPatient() {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();

        Patient patient = new Patient();
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setEmail(String.format("%s.%s@gmail.com", firstName.toLowerCase(), lastName.toLowerCase()));
        patient.setPhone(faker.phoneNumber().cellPhone());
        patient.setDateOfBirth(faker.date().birthday().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        patient.setStreet(faker.address().streetAddress());
        patient.setPostalCode(faker.address().zipCode());
        patient.setCity(faker.address().cityName());
        patient.setState(faker.address().state());
        patient.setCountry("CA");

        return patient;
    }
}
