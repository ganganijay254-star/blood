package com.bloodcare.bloodcare.repository;

import com.bloodcare.bloodcare.entity.Hospital;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Optional<Hospital> findFirstByNameIgnoreCase(String name);
}
