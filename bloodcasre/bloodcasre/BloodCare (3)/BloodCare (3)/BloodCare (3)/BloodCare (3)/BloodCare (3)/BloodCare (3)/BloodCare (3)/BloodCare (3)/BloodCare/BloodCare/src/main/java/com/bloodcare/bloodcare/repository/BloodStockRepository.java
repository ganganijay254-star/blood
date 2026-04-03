package com.bloodcare.bloodcare.repository;

import com.bloodcare.bloodcare.entity.BloodStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodStockRepository extends JpaRepository<BloodStock, Long> {
    Optional<BloodStock> findByHospitalIdAndBloodGroupIgnoreCase(Long hospitalId, String bloodGroup);

    List<BloodStock> findByHospitalId(Long hospitalId);

    List<BloodStock> findByUnitsAvailableLessThanEqual(int unitsAvailable);

    List<BloodStock> findByBloodGroupIgnoreCaseAndUnitsAvailableGreaterThan(String bloodGroup, int unitsAvailable);

    BloodStock findByBloodBankBloodBankIDAndBloodGroupIgnoreCase(Long bloodBankID, String bloodGroup);
}
