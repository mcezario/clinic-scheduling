package com.janeapp.clinicscheduling.controller.mapper;

import com.janeapp.clinicscheduling.controller.RequestValidationException;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class AppointmentTypeMapper {

    public AppointmentType fromString(final String type) {
        try {
            return AppointmentType.fromString(type);
        } catch (IllegalArgumentException e) {
            String possibleValues = Arrays.stream(AppointmentType.values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new RequestValidationException(String.format("Type should be one of: %s", possibleValues), e);
        }
    }

}
