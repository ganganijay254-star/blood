package com.bloodcare.bloodcare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.bloodcare.bloodcare.entity.BloodRequestResponse;
import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.Donor;

@Repository
public interface BloodRequestResponseRepository extends JpaRepository<BloodRequestResponse, Long> {
    List<BloodRequestResponse> findByBloodRequest(BloodRequest request);
    List<BloodRequestResponse> findByBloodRequestAndStatus(BloodRequest request, String status);
    boolean existsByBloodRequestIdAndStatus(Long bloodRequestId, String status);
    void deleteByBloodRequestIn(List<BloodRequest> requests);
    void deleteByDonor(Donor donor);
}
