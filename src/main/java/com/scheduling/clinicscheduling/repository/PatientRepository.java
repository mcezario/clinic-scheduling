package com.janeapp.clinicscheduling.repository;

import com.janeapp.clinicscheduling.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
}
