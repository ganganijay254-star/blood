package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/visit-request")
public class VisitRequestController {

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    @Autowired
    private DonorRepository donorRepository;

    // ✅ CREATE REQUEST
    @PostMapping
    public ResponseEntity<?> saveVisitRequest(
            @RequestBody VisitRequest request,
            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Please login first");
        }

        if (AdminController.isControlEnabled("maintenanceMode", false)) {
            return ResponseEntity.badRequest().body("Service temporarily unavailable due to maintenance");
        }

        if ("DONOR".equalsIgnoreCase(request.getRequestType())
                && !AdminController.isControlEnabled("enableDonations", true)) {
            return ResponseEntity.badRequest().body("Donation visits are currently disabled by admin");
        }

        int maxUnits = AdminController.getSettingInt("maxUnitsPerDonation", 2);
        if (request.getUnits() > maxUnits) {
            return ResponseEntity.badRequest().body("Maximum units per donation is " + maxUnits);
        }

        request.setUser(user);
        request.setStatus("PENDING");
        request.setRequestDate(LocalDate.now());

        VisitRequest saved = visitRequestRepository.save(request);

        return ResponseEntity.ok(saved);
    }

    // ✅ DONOR – GET MY RECENT 5
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentVisits(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Please login first");
        }

        List<VisitRequest> visits =
                visitRequestRepository
                        .findTop5ByUserOrderByRequestDateDesc(user);

        return ResponseEntity.ok(visits);
    }

    // ✅ DONOR – GET ALL MY REQUESTS
    @GetMapping("/my")
    public ResponseEntity<?> getMyRequests(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Please login first");
        }

        return ResponseEntity.ok(
                visitRequestRepository.findByUser(user)
        );
    }

    // ✅ ADMIN – GET ALL
    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                visitRequestRepository.findAll()
        );
    }

    // ✅ ADMIN – APPROVE / REJECT
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        VisitRequest req =
                visitRequestRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Not Found"));

        req.setStatus(status);
        
        // ✅ When visit is APPROVED, add units to donor
        if ("APPROVED".equalsIgnoreCase(status)) {
            User user = req.getUser();
            if (user != null) {
                Donor donor = donorRepository.findByUser(user);
                if (donor != null && req.getUnits() > 0) {
                    donor.setUnits(donor.getUnits() + req.getUnits());
                    donorRepository.save(donor);
                    System.out.println("✅ Added " + req.getUnits() + " units to donor " + donor.getId());
                }
            }
        }

        return ResponseEntity.ok(
                visitRequestRepository.save(req)
        );
    }
}
