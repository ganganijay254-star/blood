package com.bloodcare.bloodcare.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bloodcare.bloodcare.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Admin findByEmail(String email);
}