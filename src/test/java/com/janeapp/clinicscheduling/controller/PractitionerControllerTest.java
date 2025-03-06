package com.janeapp.clinicscheduling.controller;

import com.janeapp.clinicscheduling.controller.mapper.AppointmentMapper;
import com.janeapp.clinicscheduling.controller.mapper.AppointmentTypeMapper;
import com.janeapp.clinicscheduling.controller.mapper.PractitionerMapper;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Patient;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.fixture.AppointmentFixture;
import com.janeapp.clinicscheduling.entity.fixture.PatientFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import com.janeapp.clinicscheduling.repository.PatientRepository;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import com.janeapp.clinicscheduling.service.PractitionerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith( SpringRunner.class )
@WebMvcTest( PractitionerController.class )
@Import( value = {PractitionerMapper.class, AppointmentTypeMapper.class, AppointmentMapper.class} )
public class PractitionerControllerTest {

    private static final String HEADER_TIME_ZONE = "America/Los_Angeles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PractitionerService practitionerService;

    @MockitoBean
    private PractitionerRepository practitionerRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Test
    public void shouldGetPractitionerAppointmentsSuccessfully() throws Exception {
        // Given
        Long practitionerId = 1l;
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        practitioner.setId(practitionerId);
        Patient patient1 = PatientFixture.randomPatient();
        Instant date1 = Instant.now().plus(30, ChronoUnit.MINUTES);
        Appointment appointment1 = AppointmentFixture.withTimeAndPractitionerAndType(date1, practitioner, AppointmentType.INITIAL);
        appointment1.setPatient(patient1);
        Instant date2 = Instant.now().plus(30, ChronoUnit.MINUTES);
        Patient patient2 = PatientFixture.randomPatient();
        Appointment appointment2 = AppointmentFixture.withTimeAndPractitionerAndType(date2, practitioner, AppointmentType.CHECK_IN);
        appointment2.setPatient(patient2);
        List<Appointment> appointments = List.of(appointment1, appointment2);

        // Prepare
        when(practitionerRepository.findById(practitionerId))
                .thenReturn(Optional.of(practitioner));
        when(practitionerService.getCurrentDayAppointmentsByPractitioner(practitioner))
                .thenReturn(List.of(appointment1, appointment2));

        // When a request happens, then
        mockMvc.perform(get("/practitioners/" + practitionerId + "/appointments")
                        .header("Time-Zone", HEADER_TIME_ZONE)
                        .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(appointments.size())))
                .andExpect(jsonPath("$[0].patient.name", equalTo(patient1.fullName())))
                .andExpect(jsonPath("$[0].patient.email", equalTo(patient1.getEmail())))
                .andExpect(jsonPath("$[0].patient.phone", equalTo(patient1.getPhone())))
                .andExpect(jsonPath("$[0].appointmentType", equalTo(appointment1.getType().name())))
                .andExpect(jsonPath("$[0].time.start", equalTo(appointmentMapper.formatDateTime(appointment1.getStartAt(), HEADER_TIME_ZONE))))
                .andExpect(jsonPath("$[0].time.end", equalTo(appointmentMapper.formatDateTime(appointment1.getEndAt(), HEADER_TIME_ZONE))))
                .andExpect(jsonPath("$[1].patient.name", equalTo(patient2.fullName())))
                .andExpect(jsonPath("$[1].patient.email", equalTo(patient2.getEmail())))
                .andExpect(jsonPath("$[1].patient.phone", equalTo(patient2.getPhone())))
                .andExpect(jsonPath("$[1].appointmentType", equalTo(appointment2.getType().name())))
                .andExpect(jsonPath("$[1].time.start", equalTo(appointmentMapper.formatDateTime(appointment2.getStartAt(), HEADER_TIME_ZONE))))
                .andExpect(jsonPath("$[1].time.end", equalTo(appointmentMapper.formatDateTime(appointment2.getEndAt(), HEADER_TIME_ZONE))));
    }

    @Test
    public void shouldValidateInvalidPractitioner() throws Exception {
        // Given
        Long invalidId = 1l;

        // Prepare
        when(practitionerRepository.findById(invalidId))
                .thenReturn(Optional.empty());

        // When a request happens, then
        mockMvc.perform(get("/practitioners/" + invalidId + "/appointments")
                        .header("Time-Zone", HEADER_TIME_ZONE)
                        .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Practitioner not found")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.NOT_FOUND.value())));
    }

    @Test
    public void shouldHandleUnexpectedError() throws Exception {
        // Given
        Long practitionerId = 1l;
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        practitioner.setId(practitionerId);

        // Prepare
        when(practitionerRepository.findById(practitionerId))
                .thenReturn(Optional.of(practitioner));
        when(practitionerService.getCurrentDayAppointmentsByPractitioner(practitioner)).thenThrow(new RuntimeException("Boom!"));

        // When a request happens, then
        mockMvc.perform(get("/practitioners/" + practitionerId + "/appointments")
                        .header("Time-Zone", HEADER_TIME_ZONE)
                        .contentType(APPLICATION_JSON)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", equalTo("Internal Server Error")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

}