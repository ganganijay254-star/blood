package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Comparator;
import java.net.InetAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.Certificate;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.CertificateRepository;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    /* ================= GET MY CERTIFICATES ================= */
    @GetMapping("/my-certificates")
    public ResponseEntity<?> getMyCertificates(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Please login first");
        }

        try {
            ensureCertificatesForApprovedVisits(user);
            List<Certificate> certificates = certificateRepository
                    .findByDonorUserOrderByCreatedDateDesc(user);
            return ResponseEntity.ok(certificates);
        } catch (Exception e) {
            // Alternative method if user doesn't have direct reference
            Donor donor = donorRepository.findByUser(user);
            if (donor != null) {
                ensureCertificatesForApprovedVisits(user);
                List<Certificate> certificates = certificateRepository
                        .findByDonor(donor);
                certificates.sort(Comparator.comparing(
                        Certificate::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())));
                return ResponseEntity.ok(certificates);
            }
            return ResponseEntity.ok(List.of());
        }
    }

    /* ================= GET CERTIFICATE BY ID ================= */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCertificateById(@PathVariable Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElse(null);

        if (certificate == null) {
            return ResponseEntity.badRequest()
                    .body("Certificate not found");
        }

        return ResponseEntity.ok(certificate);
    }

    /* ================= VERIFY CERTIFICATE BY QR ================= */
    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<?> verifyCertificate(@PathVariable String certificateNumber) {
        
        Certificate certificate = certificateRepository
                .findByCertificateNumber(certificateNumber);

        if (certificate == null) {
            return ResponseEntity.status(404)
                    .body("Invalid certificate number");
        }

        return ResponseEntity.ok(certificate);
    }

    /* ================= GET CERTIFICATE BY NUMBER (FOR MOBILE) ================= */
    @GetMapping("/by-number/{certificateNumber}")
    public ResponseEntity<?> getCertificateByNumber(@PathVariable String certificateNumber) {
        
        Certificate certificate = certificateRepository
                .findByCertificateNumber(certificateNumber);

        if (certificate == null) {
            return ResponseEntity.status(404)
                    .body("Certificate not found");
        }

        return ResponseEntity.ok(certificate);
    }

    /* ================= RESOLVE PUBLIC BASE URL (FOR QR) ================= */
    @GetMapping("/public-base-url")
    public ResponseEntity<?> getPublicBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();

        // If opened as localhost, switch to LAN IP so phone can access same server
        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) {
                host = request.getServerName();
            }
        }

        StringBuilder base = new StringBuilder();
        base.append(scheme).append("://").append(host);
        if (!(("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443))) {
            base.append(":").append(port);
        }

        return ResponseEntity.ok(base.toString());
    }

    private void ensureCertificatesForApprovedVisits(User user) {
        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return;
        }

        List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED");
        for (VisitRequest visit : approvedVisits) {
            if ("RECEIVER".equalsIgnoreCase(visit.getRequestType())) {
                continue;
            }
            if (certificateRepository.existsByVisitRequest(visit)) {
                continue;
            }

            Certificate certificate = new Certificate();
            certificate.setDonor(donor);
            certificate.setVisitRequest(visit);
            certificate.setCertificateNumber("CERT-" + visit.getId() + "-" + System.currentTimeMillis());
            certificate.setHospitalName(visit.getHospitalName());
            certificate.setUnits(visit.getUnits());
            certificate.setDonationDate(
                    visit.getVisitDate() != null
                            ? visit.getVisitDate()
                            : (visit.getRequestDate() != null ? visit.getRequestDate() : LocalDate.now()));
            certificate.setCreatedDate(LocalDateTime.now());
            certificate.setQrCode("/verify-certificate?cert=" + certificate.getCertificateNumber());
            certificateRepository.save(certificate);
        }
    }
}
