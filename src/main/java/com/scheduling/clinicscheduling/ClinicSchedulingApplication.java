package com.janeapp.clinicscheduling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // Scans and registers @ConfigurationProperties beans
public class ClinicSchedulingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicSchedulingApplication.class, args);
    }

}
