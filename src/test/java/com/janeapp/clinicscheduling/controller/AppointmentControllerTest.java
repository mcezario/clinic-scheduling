package com.janeapp.clinicscheduling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janeapp.clinicscheduling.controller.json.AppointmentRequest;
import com.janeapp.clinicscheduling.controller.mapper.AppointmentMapper;
import com.janeapp.clinicscheduling.controller.mapper.AppointmentTypeMapper;
import com.janeapp.clinicscheduling.controller.mapper.PractitionerMapper;
import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.AppointmentType;
import com.janeapp.clinicscheduling.entity.Patient;
import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.fixture.PatientFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import com.janeapp.clinicscheduling.repository.PatientRepository;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import com.janeapp.clinicscheduling.service.AppointmentService;
import com.janeapp.clinicscheduling.service.TimeSlot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith( SpringRunner.class )
@WebMvcTest( AppointmentController.class )
@Import( value = {PractitionerMapper.class, AppointmentTypeMapper.class, AppointmentMapper.class, ObjectMapper.class} )
public class AppointmentControllerTest {

    private static final String HEADER_TIME_ZONE = "UTC";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private PractitionerRepository practitionerRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoSpyBean
    private AppointmentMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldGetAvailableSpotsSuccessfully() throws Exception {
        // Given
        AppointmentType appointmentType = AppointmentType.INITIAL;
        LocalDate date = LocalDate.now();
        Long practitionerId = 1l;
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        practitioner.setId(practitionerId);
        Instant startTime1 = Instant.now();
        Instant endTime1 = startTime1.plus(appointmentType.amount(), appointmentType.unit());
        TimeSlot timeSlot1 = new TimeSlot(startTime1, endTime1);
        Instant startTime2 = endTime1.plus(1, ChronoUnit.HOURS);
        Instant endTime2 = startTime2.plus(appointmentType.amount(), appointmentType.unit());
        TimeSlot timeSlot2 = new TimeSlot(startTime2, endTime2);
        Map<Practitioner, List<TimeSlot>> availableSpots = new HashMap<>();
        availableSpots.put(practitioner, List.of(timeSlot1, timeSlot2));

        // Prepare
        when(appointmentService.getAvailableSpotsByTypeAndDate(appointmentType, date)).thenReturn(availableSpots);

        // When a request happens, then
        mockMvc.perform(get(String.format("/appointments?type=%s&date=%s", appointmentType.name(), date.format(DateTimeFormatter.ISO_DATE)))
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(availableSpots.size())))
                .andExpect(jsonPath("$[0].practitioner.id", is(practitionerId.intValue())))
                .andExpect(jsonPath("$[0].practitioner.name", equalTo(practitioner.fullName())))
                .andExpect(jsonPath("$[0].practitioner.email", equalTo(practitioner.getEmail())))
                .andExpect(jsonPath("$[0].practitioner.phone", equalTo(practitioner.getPhone())))
                .andExpect(jsonPath("$[0].availableSlots[0].start", equalTo(mapper.formatDateTime(startTime1, HEADER_TIME_ZONE))))
                .andExpect(jsonPath("$[0].availableSlots[0].end", equalTo(mapper.formatDateTime(endTime1, HEADER_TIME_ZONE))))
                .andExpect(jsonPath("$[0].availableSlots[1].start", equalTo(mapper.formatDateTime(startTime2, HEADER_TIME_ZONE))))
                .andExpect(jsonPath("$[0].availableSlots[1].end", equalTo(mapper.formatDateTime(endTime2, HEADER_TIME_ZONE))));
    }

    @Test
    public void shouldHandleUnexpectedErrorWhenGettingAvailableSpots() throws Exception {
        // Given
        AppointmentType appointmentType = AppointmentType.INITIAL;
        LocalDate date = LocalDate.now();

        // Prepare
        doThrow(new RuntimeException("Boom!")).when(appointmentService).getAvailableSpotsByTypeAndDate(appointmentType, date);

        // When a request happens, then
        mockMvc.perform(get(String.format("/appointments?type=%s&date=%s", appointmentType.name(), date.format(DateTimeFormatter.ISO_DATE)))
                        .contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", equalTo("Internal Server Error")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    @Test
    public void shouldCreateAppointmentSuccessfully() throws Exception {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Patient patient = PatientFixture.randomPatient();
        AppointmentRequest request = createRequest(practitioner, patient);
        String requestBody = objectMapper.writeValueAsString(request);

        // Prepare
        when(practitionerRepository.findById(request.practitionerId())).thenReturn(Optional.of(practitioner));
        when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        Long appointmentId = 1l;
        Appointment mock = mock(Appointment.class);
        when(appointmentService.addAppointment(any())).thenReturn(mock);
        when(mock.getId()).thenReturn(appointmentId);

        // When a request happens, then
        mockMvc.perform(post("/appointments").contentType(APPLICATION_JSON).content(requestBody))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(appointmentId.intValue())));
    }

    @Test
    public void shouldValidateInvalidAppointmentType() throws Exception {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Patient patient = PatientFixture.randomPatient();
        AppointmentRequest request = createRequestWithInvalidAppointmentType(practitioner, patient);
        String requestBody = objectMapper.writeValueAsString(request);

        // When a request happens, then
        mockMvc.perform(post("/appointments").contentType(APPLICATION_JSON).content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("Type should be one of: INITIAL, STANDARD, CHECK_IN")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.BAD_REQUEST.value())));
    }

    @Test
    public void shouldValidateInvalidDate() throws Exception {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Patient patient = PatientFixture.randomPatient();
        AppointmentRequest request = createRequestWithInvalidDate(practitioner, patient);
        String requestBody = objectMapper.writeValueAsString(request);

        // When a request happens, then
        mockMvc.perform(post("/appointments").contentType(APPLICATION_JSON).content(requestBody))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", equalTo("Schedule time should be in ISO 8601 UTC")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.BAD_REQUEST.value())));
    }

    @Test
    public void shouldValidateInvalidPractitioner() throws Exception {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Patient patient = PatientFixture.randomPatient();
        AppointmentRequest request = createRequest(practitioner, patient);
        String requestBody = objectMapper.writeValueAsString(request);

        // Prepare
        when(practitionerRepository.findById(practitioner.getId()))
                .thenReturn(Optional.empty());

        // When a request happens, then
        mockMvc.perform(post("/appointments").contentType(APPLICATION_JSON).content(requestBody))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Practitioner not found")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.NOT_FOUND.value())));
    }

    @Test
    public void shouldValidateInvalidPatient() throws Exception {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Patient patient = PatientFixture.randomPatient();
        AppointmentRequest request = createRequest(practitioner, patient);
        String requestBody = objectMapper.writeValueAsString(request);

        // Prepare
        when(practitionerRepository.findById(practitioner.getId()))
                .thenReturn(Optional.of(practitioner));
        when(patientRepository.findById(patient.getId()))
                .thenReturn(Optional.empty());

        // When a request happens, then
        mockMvc.perform(post("/appointments").contentType(APPLICATION_JSON).content(requestBody))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", equalTo("Patient not found")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.NOT_FOUND.value())));
    }

    @Test
    public void shouldHandleUnexpectedErrorWhenCreatingAppointment() throws Exception {
        // Given
        Practitioner practitioner = PractitionerFixture.randomPractitioner();
        Patient patient = PatientFixture.randomPatient();
        AppointmentRequest request = createRequest(practitioner, patient);
        String requestBody = objectMapper.writeValueAsString(request);

        // Prepare
        doThrow(new RuntimeException("Boom!")).when(mapper).fromRequest(any(), any());

        // When a request happens, then
        mockMvc.perform(post("/appointments").contentType(APPLICATION_JSON).content(requestBody))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", equalTo("Internal Server Error")))
                .andExpect(jsonPath("$.status", equalTo(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    private AppointmentRequest createRequest(Practitioner practitioner, Patient patient) {
        return createRequest(practitioner, patient, createDate(), AppointmentType.INITIAL.name());
    }

    private AppointmentRequest createRequestWithInvalidAppointmentType(Practitioner practitioner, Patient patient) {
        return createRequest(practitioner, patient, createDate(), "invalid-type");
    }

    private AppointmentRequest createRequestWithInvalidDate(Practitioner practitioner, Patient patient) {
        return createRequest(practitioner, patient, "invalid-date", AppointmentType.INITIAL.name());
    }

    private AppointmentRequest createRequest(Practitioner practitioner, Patient patient, String date, String appointmentType) {
        Long patientId = 1l;
        patient.setId(patientId);
        Long practitionerId = 1l;
        practitioner.setId(practitionerId);

        return new AppointmentRequest(patientId, practitionerId, date, appointmentType, null);
    }

    private String createDate() {
        return Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
    }

}
