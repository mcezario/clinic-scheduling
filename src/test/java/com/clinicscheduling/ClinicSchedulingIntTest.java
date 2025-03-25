package com.janeapp.clinicscheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.janeapp.clinicscheduling.controller.json.AppointmentRequest;
import com.janeapp.clinicscheduling.entity.*;
import com.janeapp.clinicscheduling.entity.fixture.AppointmentFixture;
import com.janeapp.clinicscheduling.entity.fixture.PatientFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerFixture;
import com.janeapp.clinicscheduling.entity.fixture.PractitionerUnavailabilityFixture;
import com.janeapp.clinicscheduling.repository.AppointmentRepository;
import com.janeapp.clinicscheduling.repository.PatientRepository;
import com.janeapp.clinicscheduling.repository.PractitionerRepository;
import com.janeapp.clinicscheduling.repository.PractitionerUnavailabilityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ClinicSchedulingIntTest extends BaseIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private PractitionerUnavailabilityRepository practitionerUnavailabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TOKYO_TIMEZONE = "Asia/Tokyo";

    private static final String VANCOUVER_TIMEZONE = "America/Vancouver";

    private static final ZoneId CLINIC_ZONE = ZoneId.of(VANCOUVER_TIMEZONE);

    private static final ZoneId TOKYO_ZONE = ZoneId.of(TOKYO_TIMEZONE);

    private Practitioner practitioner;

    private Patient patient;

    @BeforeEach
    public void setUp() {
        practitioner = practitionerRepository.save(PractitionerFixture.randomPractitioner());
        patient = patientRepository.save(PatientFixture.randomPatient());
    }

    @AfterEach
    public void tearDown() {
        appointmentRepository.deleteAll();
        practitionerRepository.deleteAll();
        patientRepository.deleteAll();
        patientRepository.deleteAll();
        practitionerUnavailabilityRepository.deleteAll();
    }

    @Test
    public void testSchedulingJourney() throws Exception {
        // Given
        LocalDate futureDate = LocalDate.now().plus(1, ChronoUnit.DAYS);
        LocalDateTime appointmentClinicTimeAt11am = LocalDateTime.of(futureDate, LocalTime.of(11, 0));
        ZonedDateTime clinicZonedDateTime = ZonedDateTime.of(appointmentClinicTimeAt11am, CLINIC_ZONE);
        ZonedDateTime tokyoZonedDateTime = clinicZonedDateTime.withZoneSameInstant(TOKYO_ZONE);

        // Sets practitioners unavailability
        Instant timeOffStart = clinicZonedDateTime.withHour(9).withZoneSameInstant(ZoneOffset.UTC).toInstant();
        Instant timeOffEnd = clinicZonedDateTime.withHour(10).withZoneSameInstant(ZoneOffset.UTC).toInstant();
        PractitionerUnavailability practitionerUnavailability = PractitionerUnavailabilityFixture.withTime(practitioner, timeOffStart, timeOffEnd);
        practitionerUnavailabilityRepository.save(practitionerUnavailability);

        // Creates appointment details
        String apptInClinicTimeZoneAt11am = tokyoZonedDateTime.format(DateTimeFormatter.ISO_INSTANT);
        AppointmentType appointmentType = AppointmentType.STANDARD;
        AppointmentRequest appointment = new AppointmentRequest(patient.getId(), practitioner.getId(),
                apptInClinicTimeZoneAt11am, appointmentType.name(), "Appointment from Tokyo!");
        String request = objectMapper.writeValueAsString(appointment);

        // Create an appointment for a patient located in Tokyo
        mockMvc.perform(post("/appointments").header("Time-Zone", TOKYO_TIMEZONE).contentType(APPLICATION_JSON)
                        .content(request))
                .andDo(print())
                .andExpect(status().isOk());

        // Then returns available spots locking the time for an appointment slot already taken
        String date = Instant.parse(apptInClinicTimeZoneAt11am).atZone(CLINIC_ZONE).toLocalDate().format(DateTimeFormatter.ISO_DATE);
        mockMvc.perform(get("/appointments?type=" + appointmentType.name() + "&date=" + date).header("Time-Zone", TOKYO_TIMEZONE).contentType(APPLICATION_JSON)
                        .content(request))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].practitioner.id", is(practitioner.getId().intValue())))
                .andExpect(jsonPath("$[0].practitioner.name", equalTo(practitioner.fullName())))
                .andExpect(jsonPath("$[0].practitioner.email", equalTo(practitioner.getEmail())))
                .andExpect(jsonPath("$[0].practitioner.phone", equalTo(practitioner.getPhone())))
                .andExpect(jsonPath("$[0].availableSlots", hasSize(6))) // Below we make sure 04:00 - 05:00 do not return
                // Below we make sure 04:00 - 05:00 (time taken for appointment) and 02:00 - 03:00 (practitioners unavailability) do not return
                .andExpect(jsonPath("$[0].availableSlots[0].start", equalTo(formatTime(tokyoZonedDateTime, "03:00"))))
                .andExpect(jsonPath("$[0].availableSlots[0].end", equalTo(formatTime(tokyoZonedDateTime, "04:00"))))
                .andExpect(jsonPath("$[0].availableSlots[1].start", equalTo(formatTime(tokyoZonedDateTime, "05:00"))))
                .andExpect(jsonPath("$[0].availableSlots[1].end", equalTo(formatTime(tokyoZonedDateTime, "06:00"))))
                .andExpect(jsonPath("$[0].availableSlots[2].start", equalTo(formatTime(tokyoZonedDateTime, "06:00"))))
                .andExpect(jsonPath("$[0].availableSlots[2].end", equalTo(formatTime(tokyoZonedDateTime, "07:00"))))
                .andExpect(jsonPath("$[0].availableSlots[3].start", equalTo(formatTime(tokyoZonedDateTime, "07:00"))))
                .andExpect(jsonPath("$[0].availableSlots[3].end", equalTo(formatTime(tokyoZonedDateTime, "08:00"))))
                .andExpect(jsonPath("$[0].availableSlots[4].start", equalTo(formatTime(tokyoZonedDateTime, "08:00"))))
                .andExpect(jsonPath("$[0].availableSlots[4].end", equalTo(formatTime(tokyoZonedDateTime, "09:00"))))
                .andExpect(jsonPath("$[0].availableSlots[5].start", equalTo(formatTime(tokyoZonedDateTime, "09:00"))))
                .andExpect(jsonPath("$[0].availableSlots[5].end", equalTo(formatTime(tokyoZonedDateTime, "10:00"))));


        // Also, make sure dates return in different timezones
        mockMvc.perform(get("/appointments?type=" + appointmentType.name() + "&date=" + date).header("Time-Zone", VANCOUVER_TIMEZONE).contentType(APPLICATION_JSON)
                        .content(request))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].practitioner.id", is(practitioner.getId().intValue())))
                .andExpect(jsonPath("$[0].practitioner.name", equalTo(practitioner.fullName())))
                .andExpect(jsonPath("$[0].practitioner.email", equalTo(practitioner.getEmail())))
                .andExpect(jsonPath("$[0].practitioner.phone", equalTo(practitioner.getPhone())))
                .andExpect(jsonPath("$[0].availableSlots", hasSize(6)))
                // Below we make sure 11:00 - 12:00 (time taken for appointment) and 09:00 - 10:00 (practitioners unavailability) do not return
                .andExpect(jsonPath("$[0].availableSlots[0].start", equalTo(formatTime(clinicZonedDateTime, "10:00"))))
                .andExpect(jsonPath("$[0].availableSlots[0].end", equalTo(formatTime(clinicZonedDateTime, "11:00"))))
                .andExpect(jsonPath("$[0].availableSlots[1].start", equalTo(formatTime(clinicZonedDateTime, "12:00"))))
                .andExpect(jsonPath("$[0].availableSlots[1].end", equalTo(formatTime(clinicZonedDateTime, "13:00"))))
                .andExpect(jsonPath("$[0].availableSlots[2].start", equalTo(formatTime(clinicZonedDateTime, "13:00"))))
                .andExpect(jsonPath("$[0].availableSlots[2].end", equalTo(formatTime(clinicZonedDateTime, "14:00"))))
                .andExpect(jsonPath("$[0].availableSlots[3].start", equalTo(formatTime(clinicZonedDateTime, "14:00"))))
                .andExpect(jsonPath("$[0].availableSlots[3].end", equalTo(formatTime(clinicZonedDateTime, "15:00"))))
                .andExpect(jsonPath("$[0].availableSlots[4].start", equalTo(formatTime(clinicZonedDateTime, "15:00"))))
                .andExpect(jsonPath("$[0].availableSlots[4].end", equalTo(formatTime(clinicZonedDateTime, "16:00"))))
                .andExpect(jsonPath("$[0].availableSlots[5].start", equalTo(formatTime(clinicZonedDateTime, "16:00"))))
                .andExpect(jsonPath("$[0].availableSlots[5].end", equalTo(formatTime(clinicZonedDateTime, "17:00"))));
    }

    @Test
    public void testPractitionerAppointments() throws Exception {
        // Given
        LocalDate futureDate = LocalDate.now();
        LocalDateTime appointmentClinicTimeAt11am = LocalDateTime.of(futureDate, LocalTime.of(11, 0));
        ZonedDateTime clinicZonedDateTime = ZonedDateTime.of(appointmentClinicTimeAt11am, CLINIC_ZONE);
        ZonedDateTime tokyoZonedDateTime = clinicZonedDateTime.withZoneSameInstant(TOKYO_ZONE);
        ZonedDateTime utcZonedDateTime = clinicZonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
        AppointmentType appointmentType = AppointmentType.INITIAL;
        Appointment appointment = AppointmentFixture.withAllDetails(utcZonedDateTime.toInstant(), appointmentType, patient, practitioner);
        appointmentRepository.save(appointment);

        // Returns all schedules for the given practitioner
        mockMvc.perform(get("/practitioners/" + practitioner.getId() + "/appointments").header("Time-Zone", TOKYO_TIMEZONE).contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patient.name", equalTo(patient.fullName())))
                .andExpect(jsonPath("$[0].patient.email", equalTo(patient.getEmail())))
                .andExpect(jsonPath("$[0].patient.phone", equalTo(patient.getPhone())))
                .andExpect(jsonPath("$[0].appointmentType", equalTo(appointmentType.name())))
                .andExpect(jsonPath("$[0].time.start", equalTo(formatTime(tokyoZonedDateTime, "04:00"))))
                .andExpect(jsonPath("$[0].time.end", equalTo(formatTime(tokyoZonedDateTime, "05:30"))));


        // Also, make sure dates return in different timezones
        mockMvc.perform(get("/practitioners/" + practitioner.getId() + "/appointments").header("Time-Zone", VANCOUVER_TIMEZONE).contentType(APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patient.name", equalTo(patient.fullName())))
                .andExpect(jsonPath("$[0].patient.email", equalTo(patient.getEmail())))
                .andExpect(jsonPath("$[0].patient.phone", equalTo(patient.getPhone())))
                .andExpect(jsonPath("$[0].appointmentType", equalTo(appointmentType.name())))
                .andExpect(jsonPath("$[0].time.start", equalTo(formatTime(clinicZonedDateTime, "11:00"))))
                .andExpect(jsonPath("$[0].time.end", equalTo(formatTime(clinicZonedDateTime, "12:30"))));
    }

    private String formatTime(ZonedDateTime zonedDateTime, String time) {
        return String.format("%s %s", zonedDateTime.toLocalDate().format(DateTimeFormatter.ISO_DATE), time);
    }

}
