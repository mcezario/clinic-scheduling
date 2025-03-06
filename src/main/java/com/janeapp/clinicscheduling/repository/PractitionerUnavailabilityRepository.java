package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.entity.Practitioner;
import com.janeapp.clinicscheduling.entity.PractitionerUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PractitionerUnavailabilityRepository extends JpaRepository<PractitionerUnavailability, Long> {

    @Query( "SELECT COUNT(pu) > 0 FROM PractitionerUnavailability pu " +
            "WHERE :date BETWEEN pu.startAt AND pu.endAt " +
            "AND pu.practitioner = :practitioner" )
    boolean existsUnavailabilityForTimeRange(@Param( "date" ) Instant date,
                                             @Param( "practitioner" ) Practitioner practitioner);

    @Query( "SELECT pu FROM PractitionerUnavailability pu " +
            "WHERE pu.startAt >= :start AND pu.endAt <= :end " )
    List<PractitionerUnavailability> findUnavailabilityForTimeRange(@Param( "start" ) Instant start,
                                                                    @Param( "end" ) Instant end);

}
