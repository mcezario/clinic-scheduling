package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PractitionerRepository extends JpaRepository<Practitioner, Long> {
}
