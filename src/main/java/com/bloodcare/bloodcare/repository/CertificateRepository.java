package com.bloodcare.bloodcare.repository;

import com.bloodcare.bloodcare.entity.Certificate;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    
    List<Certificate> findByDonor(Donor donor);
    
    List<Certificate> findByDonorUserOrderByCreatedDateDesc(User user);
    
    Certificate findByCertificateNumber(String certificateNumber);

    boolean existsByVisitRequest(VisitRequest visitRequest);

    void deleteByDonor(Donor donor);

    void deleteByVisitRequestIn(List<VisitRequest> visitRequests);
}
