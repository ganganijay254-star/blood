package com.bloodcare.bloodcare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.Donor;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    
    List<BloodRequest> findByUser(User user);
    
    List<BloodRequest> findByStatus(String status);
    
    List<BloodRequest> findByBloodGroupAndStatusOrderByUrgencyDesc(String bloodGroup, String status);
    
    List<BloodRequest> findByStatusOrderByCreatedDateDesc(String status);
    
    List<BloodRequest> findByUrgencyAndStatusOrderByCreatedDateDesc(String urgency, String status);

    List<BloodRequest> findByConfirmedDonor(Donor donor);

    void deleteByUser(User user);
}
