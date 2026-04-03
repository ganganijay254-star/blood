package com.bloodcare.bloodcare.repository;

import java.util.List; // ✅ CORRECT

import org.springframework.data.jpa.repository.JpaRepository;

import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.entity.User;

public interface VisitRequestRepository
extends JpaRepository<VisitRequest, Long> {

	List<VisitRequest> findTop5ByUserOrderByRequestDateDesc(User user);

	List<VisitRequest> findByUser(User user);
	
	List<VisitRequest> findByUserAndStatus(User user, String status);

	void deleteByUser(User user);
}
