package com.bloodcare.bloodcare.repository;

import com.bloodcare.bloodcare.entity.BloodBank;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodBankRepository extends JpaRepository<BloodBank, Long> {
    List<BloodBank> findByLocationIgnoreCase(String location);

    List<BloodBank> findByBloodGroupIgnoreCase(String bloodGroup);

    List<BloodBank> findByLocationIgnoreCaseAndBloodGroupIgnoreCase(String location, String bloodGroup);

    List<BloodBank> findByHospitalId(Long hospitalId);
}
