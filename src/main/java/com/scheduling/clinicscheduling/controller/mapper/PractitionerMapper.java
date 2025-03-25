package com.janeapp.clinicscheduling.controller.mapper;

import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

@Component
public class PractitionerMapper {

    @Autowired
    private PractitionerRepository practitionerRepository;

    public Practitioner fromId(final Long id) {
        return practitionerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Practitioner not found"));
    }

}
