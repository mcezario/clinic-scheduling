package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.entity.Appointment;
import com.janeapp.clinicscheduling.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query( "SELECT COUNT(a) > 0 FROM Appointment a " +
            "WHERE :date BETWEEN a.startAt AND a.endAt " +
            "AND a.practitioner = :practitioner" )
    boolean existsAppointmentInTimeRange(@Param( "date" ) Instant date,
                                         @Param( "practitioner" ) Practitioner practitioner);

    @Query( "SELECT a FROM Appointment a " +
            "WHERE a.practitioner = :practitioner AND (" +
            " (a.startAt BETWEEN :start AND :end) " +
            " OR (a.endAt BETWEEN :start AND :end) " +
            " OR (a.startAt <= :start AND a.endAt >= :end)" +
            ") " )
    List<Appointment> findPractitionerAppointmentsByTimeRange(@Param( "start" ) Instant start,
                                                              @Param( "end" ) Instant end,
                                                              @Param( "practitioner" ) Practitioner practitioner);

    @Query( value = "SELECT a.* FROM appointment a " +
            "WHERE a.start_at >= :start AND a.end_at <= :end " +
            "AND a.appointment_type = CAST(:type AS appointment_type_enum) ",
            nativeQuery = true )
    List<Appointment> findAppointmentsByRangeAndType(@Param( "type" ) String type,
                                                     @Param( "start" ) Instant start,
                                                     @Param( "end" ) Instant end);

}
