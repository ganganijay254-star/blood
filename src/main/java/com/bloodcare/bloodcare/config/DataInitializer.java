package com.bloodcare.bloodcare.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.bloodcare.bloodcare.entity.Admin;
import com.bloodcare.bloodcare.repository.AdminRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // Check if admin already exists
        if (adminRepository.findByEmail("admin@gmail.com") == null) {

            Admin admin = new Admin();
            admin.setName("Super Admin");
            admin.setEmail("admin@gmail.com");

            // 🔥 FIXED PASSWORD
            admin.setPassword(passwordEncoder.encode("1234"));

            adminRepository.save(admin);

            System.out.println("✅ Default Admin Created");
        } else {
            System.out.println("ℹ Admin Already Exists");
        }
    }
}