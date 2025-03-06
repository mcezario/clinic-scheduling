package com.janeapp.clinicscheduling.service;

import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

@ExtendWith( MockitoExtension.class )
public class PractitionerServiceTest {

    @Mock
    private PractitionerRepository practitionerRepository;

    @InjectMocks
    private PractitionerService practitionerService;

    @Test
    void shouldCreateAppointmentSuccessfully() {
        // Given
        Practitioner practitioner1 = PractitionerFixture.randomPractitioner();
        Practitioner practitioner2 = PractitionerFixture.randomPractitioner();
        when(practitionerRepository.findAll()).thenReturn(List.of(practitioner1, practitioner2));

        // When
        List<Practitioner> practitioners = practitionerRepository.findAll();

        // Then
        assertThat(practitioners, hasSize(2));
        assertThat(practitioners, containsInAnyOrder(practitioner1, practitioner2));
    }

}
