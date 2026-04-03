package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.BloodRequestResponse;
import com.bloodcare.bloodcare.repository.BloodRequestRepository;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.service.BloodStockService;
import com.bloodcare.bloodcare.service.BloodRequestService;
import com.bloodcare.bloodcare.service.NotificationService;
import com.bloodcare.bloodcare.service.DonorPriorityScoreService;

@RestController
@RequestMapping("/api/blood-request")
public class BloodRequestController {

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private com.bloodcare.bloodcare.repository.UserRepository userRepository;

    @Autowired
    private com.bloodcare.bloodcare.service.SmartEligibilityCheckerService checkerService;

    @Autowired
    private com.bloodcare.bloodcare.repository.BloodRequestResponseRepository responseRepository;

    @Autowired
    private com.bloodcare.bloodcare.repository.NotificationRepository notificationRepository;

    @Autowired
    private com.bloodcare.bloodcare.service.EmailService emailService;

    @Autowired
    private DonorPriorityScoreService donorPriorityScoreService;

    @Autowired
    private BloodStockService bloodStockService;

    @Autowired
    private BloodRequestService bloodRequestService;

    /* ===== CREATE BLOOD REQUEST ===== */
    @PostMapping("/create")
    public ResponseEntity<?> createBloodRequest(
            @RequestBody BloodRequest request,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        if (AdminController.isControlEnabled("maintenanceMode", false)) {
            return ResponseEntity.badRequest().body("Service temporarily unavailable due to maintenance");
        }
        if (!AdminController.isControlEnabled("showBloodRequest", true)
                || !AdminController.isControlEnabled("enableRequests", true)) {
            return ResponseEntity.badRequest().body("Blood request feature is currently disabled by admin");
        }

        try {
            if (request.getCity() != null) request.setCity(request.getCity().trim());
            if (request.getBloodGroup() != null) request.setBloodGroup(request.getBloodGroup().trim().toUpperCase());
            if (request.getUrgency() != null) request.setUrgency(request.getUrgency().trim().toUpperCase());
            request.setUser(user);
            request.setStatus("PENDING_APPROVAL");
            request.setVerified(false);
            request.setCreatedDate(LocalDateTime.now());
            
            BloodRequest saved = bloodRequestRepository.save(request);
            // Generate a public id for tracking
            saved.setPublicId("BR-" + (1000 + saved.getId()));
            saved = bloodRequestRepository.save(saved);

            // If critical, notify admins immediately and run smart matching to top donors
            if ("CRITICAL".equalsIgnoreCase(saved.getUrgency())) {
                // Notify all admin users
                List<User> admins = userRepository.findAll().stream()
                        .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()))
                        .collect(Collectors.toList());
                for (User admin : admins) {
                    notificationService.createNotification(admin, "CRITICAL Blood Request", "A critical blood request " + saved.getPublicId() + " was submitted.", "ALERT");
                }

                // Already published and matched above; admin alerts are still sent for critical
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", saved);
            response.put("compatibleBloodGroups", bloodStockService.getCompatibleBloodGroups(saved.getBloodGroup()));
            response.put("message", "Blood request submitted. Hospital approval is pending.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating request: " + e.getMessage());
        }
    }

    @PostMapping("/create-emergency")
    public ResponseEntity<?> createEmergencyRequest(@RequestBody BloodRequest request, HttpSession session) {
        request.setUrgency("CRITICAL");
        return createBloodRequest(request, session);
    }

    /* ===== GET OPEN BLOOD REQUESTS WITH DONOR PRIORITY ===== */
    @GetMapping("/open")
    public ResponseEntity<?> getOpenRequests() {
        try {
            // Only return requests that are verified and open
            List<BloodRequest> openRequests = bloodRequestRepository
                .findByStatusOrderByCreatedDateDesc("OPEN");
            
            // Sort by urgency priority
            List<BloodRequest> prioritized = openRequests.stream()
                .sorted((a, b) -> {
                    int urgencyOrder = getUrgencyOrder(b.getUrgency()) - getUrgencyOrder(a.getUrgency());
                    if (urgencyOrder != 0) return urgencyOrder;
                    return b.getCreatedDate().compareTo(a.getCreatedDate());
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(prioritized);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching requests");
        }
    }

    /* ===== ADMIN: VERIFY + PUBLISH REQUEST ===== */
    @PostMapping("/verify/{requestId}")
    public ResponseEntity<?> verifyRequest(@PathVariable Long requestId) {
        try {
            BloodRequest request = bloodRequestRepository.findById(requestId).orElse(null);
            if (request == null) return ResponseEntity.badRequest().body("Request not found");

            request.setVerified(true);
            request.setStatus("OPEN");
            request.setApprovedDate(LocalDateTime.now());
            bloodRequestRepository.save(request);

            // Run matching and notify top donors when published
            List<Map<String, Object>> notified = checkerService.matchAndNotify(request.getBloodGroup(), request.getCity(), request.getUnitsRequired(), 10);
            request.setMatchedDonorsCount(notified.size());
            bloodRequestRepository.save(request);
            List<Map<String, Object>> emergencyBroadcast = List.of();
            if (notified.isEmpty()) {
                emergencyBroadcast = checkerService.sendEmergencyNotificationToAll(
                        request.getBloodGroup(),
                        request.getCity(),
                        request.getPublicId());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notified", notified.size(),
                    "emergencyBroadcastCount", emergencyBroadcast.size(),
                    "request", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error verifying request: " + e.getMessage());
        }
    }

    /* ===== DONOR RESPONSE (ACCEPT / DECLINE) ===== */
    @PostMapping("/respond/{requestId}/{donorId}")
    public ResponseEntity<?> donorRespond(@PathVariable Long requestId, @PathVariable Long donorId, @RequestParam String action) {
        try {
            BloodRequest request = bloodRequestRepository.findById(requestId).orElse(null);
            Donor donor = donorRepository.findById(donorId).orElse(null);
            if (request == null || donor == null) return ResponseEntity.badRequest().body("Invalid request or donor");

            if ("COMPLETED".equalsIgnoreCase(request.getStatus())
                    || "FULFILLED".equalsIgnoreCase(request.getStatus())
                    || "CANCELLED".equalsIgnoreCase(request.getStatus())
                    || "REJECTED".equalsIgnoreCase(request.getStatus())) {
                return ResponseEntity.badRequest().body("Request already closed");
            }

            String status = "PENDING";
            if ("accept".equalsIgnoreCase(action)) status = "ACCEPTED";
            if ("decline".equalsIgnoreCase(action)) status = "DECLINED";

            if ("ACCEPTED".equalsIgnoreCase(status)
                    && request.getConfirmedDonor() != null
                    && !request.getConfirmedDonor().getId().equals(donorId)) {
                return ResponseEntity.status(409).body("Request already fulfilled by another donor");
            }

            BloodRequestResponse existing = responseRepository.findByBloodRequest(request).stream()
                    .filter(r -> r.getDonor() != null && r.getDonor().getId().equals(donorId))
                    .findFirst().orElse(null);

            boolean isNew = false;
            if (existing == null) {
                BloodRequestResponse resp = new BloodRequestResponse(request, donor, status);
                responseRepository.save(resp);
                isNew = true;
            } else {
                existing.setStatus(status);
                existing.setResponseDate(LocalDateTime.now());
                responseRepository.save(existing);
            }

            if ("ACCEPTED".equalsIgnoreCase(status)) {
                request.setConfirmedDonor(donor);
                request.setStatus("DONOR_ASSIGNED");

                // Notify receiver and admin
                if (request.getUser() != null) {
                    String donorName = donor.getUser() != null ? donor.getUser().getName() : "Donor";
                    String donorPhone = donor.getUser() != null ? donor.getUser().getMobile() : "Not available";
                    String donorCity = donor.getCity() == null || donor.getCity().isBlank() ? "Not available" : donor.getCity();
                    notificationService.createNotification(
                            request.getUser(),
                            "Donor Assigned",
                            "Good news! A donor has accepted your request. Donor Name: " + donorName
                                    + ", Blood Group: " + (donor.getBloodGroup() == null ? "-" : donor.getBloodGroup())
                                    + ", Contact: " + donorPhone
                                    + ", Location: " + donorCity,
                            "BLOOD_REQUEST");
                    // Send detailed acceptance email to requester
                    if (request.getUser().getEmail() != null && !request.getUser().getEmail().isBlank()) {
                        try {
                            emailService.sendDonorAcceptedEmail(request.getUser().getEmail(), request.getUser().getName(), request, donor);
                        } catch (Exception mailError) {
                            System.out.println("Donor assignment email failed: " + mailError.getMessage());
                        }
                    }
                }
                List<User> admins = userRepository.findAll().stream().filter(u -> "ADMIN".equalsIgnoreCase(u.getRole())).collect(Collectors.toList());
                for (User admin : admins) {
                    notificationService.createNotification(admin, "Donor Assigned", "Donor " + donor.getUser().getName() + " assigned to request " + request.getPublicId(), "ALERT");
                }
            }

            request.setMatchedDonorsCount(responseRepository.findByBloodRequestAndStatus(request, "ACCEPTED").size());
            request.setDonorsRespondedCount(responseRepository.findByBloodRequest(request).size());
            if ("DECLINED".equalsIgnoreCase(status)) {
                request = bloodRequestService.processRequest(request.getId());
            } else {
                bloodRequestRepository.save(request);
            }

            return ResponseEntity.ok(Map.of("success", true, "status", status, "request", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error responding to request: " + e.getMessage());
        }
    }

    // Allow donor to respond using session (infer donor from logged-in user)
    @PostMapping("/respond/{requestId}")
    public ResponseEntity<?> donorRespondBySession(@PathVariable Long requestId, @RequestParam String action, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) return ResponseEntity.status(401).body("Please login first");

            Donor donor = donorRepository.findByUser(user);
            if (donor == null) return ResponseEntity.badRequest().body("Donor profile not found");

            // Delegate to existing logic by reusing donorRespond flow
            return donorRespond(requestId, donor.getId(), action);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error responding to request: " + e.getMessage());
        }
    }

    /* ===== UPLOAD MEDICAL PROOF FOR A REQUEST ===== */
    @PostMapping("/upload-proof/{requestId}")
    public ResponseEntity<?> uploadMedicalProof(@PathVariable Long requestId,
                                                @RequestParam("file") MultipartFile file,
                                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Please login first");

        try {
            BloodRequest request = bloodRequestRepository.findById(requestId).orElse(null);
            if (request == null) return ResponseEntity.badRequest().body("Request not found");
            if (!request.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).body("Not allowed");

            if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("No file uploaded");

            // Ensure uploads directory exists
            java.nio.file.Path uploadsDir = java.nio.file.Paths.get("uploads/medical-proofs");
            java.nio.file.Files.createDirectories(uploadsDir);

            String original = java.nio.file.Paths.get(file.getOriginalFilename()).getFileName().toString();
            String safeName = java.time.Instant.now().toEpochMilli() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");

            java.nio.file.Path dest = uploadsDir.resolve(safeName);
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String publicPath = "/uploads/medical-proofs/" + safeName;
            request.setMedicalProofPath(publicPath);
            bloodRequestRepository.save(request);

            return ResponseEntity.ok(Map.of("success", true, "path", publicPath));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error uploading file: " + e.getMessage());
        }
    }

    /* ===== TRACK A REQUEST (Receiver view) ===== */
    @GetMapping("/track/{requestId}")
    public ResponseEntity<?> trackRequest(@PathVariable Long requestId) {
        try {
            BloodRequest request = bloodRequestRepository.findById(requestId).orElse(null);
            if (request == null) return ResponseEntity.badRequest().body("Request not found");

            List<BloodRequestResponse> responses = responseRepository.findByBloodRequest(request);
            if ((responses.isEmpty() || !responseRepository.existsByBloodRequestIdAndStatus(requestId, "ACCEPTED"))
                    && request.getConfirmedDonor() == null
                    && ("OPEN".equalsIgnoreCase(request.getStatus())
                        || "IN_PROGRESS".equalsIgnoreCase(request.getStatus())
                        || "RESERVED".equalsIgnoreCase(request.getStatus()))) {
                request = bloodRequestService.processRequest(requestId);
                responses = responseRepository.findByBloodRequest(request);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("publicId", request.getPublicId());
            data.put("status", request.getStatus());
            data.put("matchedDonors", request.getMatchedDonorsCount());
            data.put("donorsResponded", request.getDonorsRespondedCount());
            data.put("bloodGroup", request.getBloodGroup());
            data.put("hospital", request.getHospital());
            data.put("urgency", request.getUrgency());
            data.put("unitsRequired", request.getUnitsRequired());
            data.put("multiSourceAvailability",
                    bloodStockService.getMultiSourceAvailability(request.getHospital(), request.getBloodGroup(), request.getCity()));
            if (request.getConfirmedDonor() != null) {
                Map<String, Object> assignedDonor = new HashMap<>();
                assignedDonor.put("id", request.getConfirmedDonor().getId());
                assignedDonor.put("name", request.getConfirmedDonor().getUser() != null ? request.getConfirmedDonor().getUser().getName() : "Donor");
                assignedDonor.put("phone", request.getConfirmedDonor().getUser() != null ? request.getConfirmedDonor().getUser().getMobile() : null);
                assignedDonor.put("location", request.getConfirmedDonor().getCity());
                assignedDonor.put("bloodGroup", request.getConfirmedDonor().getBloodGroup());
                data.put("assignedDonor", assignedDonor);
            } else {
                data.put("assignedDonor", null);
            }
            data.put("compatibleBloodGroups", bloodStockService.getCompatibleBloodGroups(request.getBloodGroup()));
            data.put("stockSnapshot", bloodStockService.getHospitalStockSnapshot(request.getHospital(), request.getBloodGroup()));
            data.put("responses", responses.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("donorId", r.getDonor().getId());
                m.put("name", r.getDonor().getUser().getName());
                m.put("status", r.getStatus());
                m.put("responseDate", r.getResponseDate());
                return m;
            }).collect(Collectors.toList()));

            // include medical proof link if present
            data.put("medicalProofPath", request.getMedicalProofPath());

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error tracking request: " + e.getMessage());
        }
    }

    /* ===== GET MATCHING REQUESTS FOR LOGGED IN DONOR ===== */
    @GetMapping("/matching-for-donor")
    public ResponseEntity<?> getMatchingRequestsForDonor(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Please login first");

        Donor donor = donorRepository.findByUser(user);
        if (donor == null) return ResponseEntity.ok(List.of());

        try {
            List<BloodRequest> open = bloodRequestRepository.findAll().stream()
                    .filter(req -> req.getStatus() != null)
                    .filter(BloodRequest::isVerified)
                    .filter(req -> {
                        String status = req.getStatus().toUpperCase();
                        return "OPEN".equals(status)
                                || "SUBMITTED".equals(status)
                                || "IN_PROGRESS".equals(status)
                                || "RESERVED".equals(status);
                    })
                    .collect(Collectors.toList());
            List<Map<String, Object>> matches = new java.util.ArrayList<>();
            String donorBloodGroup = donor.getBloodGroup() == null ? "" : donor.getBloodGroup().trim().toLowerCase();
            String donorCity = donor.getCity() == null ? "" : donor.getCity().trim().toLowerCase();

            for (BloodRequest req : open) {
                String reqBloodGroup = req.getBloodGroup() == null ? "" : req.getBloodGroup().trim().toLowerCase();
                String reqCity = req.getCity() == null ? "" : req.getCity().trim().toLowerCase();
                List<String> compatibleGroups = bloodStockService.getCompatibleBloodGroups(reqBloodGroup);
                if (reqBloodGroup.isEmpty() || donorBloodGroup.isEmpty() || !compatibleGroups.contains(donorBloodGroup.toUpperCase())) continue;

                boolean responded = responseRepository.findByBloodRequest(req).stream()
                        .anyMatch(r -> r.getDonor() != null && r.getDonor().getId().equals(donor.getId()));

                Map<String, Object> m = new HashMap<>();
                m.put("id", req.getId());
                m.put("publicId", req.getPublicId());
                m.put("patientName", req.getPatientName());
                m.put("hospital", req.getHospital());
                m.put("unitsRequired", req.getUnitsRequired());
                m.put("urgency", req.getUrgency());
                m.put("city", req.getCity());
                m.put("sameCity", !reqCity.isEmpty() && !donorCity.isEmpty() && reqCity.equals(donorCity));
                m.put("status", req.getStatus());
                m.put("contactNumber", req.getContactNumber());
                m.put("matchedDonors", req.getMatchedDonorsCount());
                m.put("donorsResponded", req.getDonorsRespondedCount());
                m.put("responded", responded);
                matches.add(m);
            }

            matches.sort((a, b) -> {
                boolean aSameCity = Boolean.TRUE.equals(a.get("sameCity"));
                boolean bSameCity = Boolean.TRUE.equals(b.get("sameCity"));
                if (aSameCity != bSameCity) {
                    return bSameCity ? 1 : -1;
                }

                int urgencyA = urgencyRank((String) a.get("urgency"));
                int urgencyB = urgencyRank((String) b.get("urgency"));
                if (urgencyA != urgencyB) {
                    return Integer.compare(urgencyB, urgencyA);
                }

                Long aId = ((Number) a.get("id")).longValue();
                Long bId = ((Number) b.get("id")).longValue();
                return Long.compare(bId, aId);
            });

            return ResponseEntity.ok(matches);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching matching requests: " + e.getMessage());
        }
    }

    private int urgencyRank(String urgency) {
        if (urgency == null) return 0;
        return switch (urgency.trim().toUpperCase()) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    /* ===== GET MATCHING DONORS FOR BLOOD REQUEST ===== */
    @GetMapping("/matching-donors/{requestId}")
    public ResponseEntity<?> getMatchingDonors(@PathVariable Long requestId) {
        try {
            BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                return ResponseEntity.badRequest().body("Request not found");
            }
            
            // Get donors with same blood group
            List<Donor> allDonors = donorRepository.findAll();
            
            // Prioritize previous donors, then matching blood group
            List<Map<String, Object>> matchedDonors = allDonors.stream()
                .filter(d -> d.getUser() != null && d.getBloodGroup() != null 
                    && d.getBloodGroup().equalsIgnoreCase(request.getBloodGroup()))
                .map(d -> {
                    Map<String, Object> donorInfo = new HashMap<>();
                    donorInfo.put("id", d.getId());
                    donorInfo.put("name", d.getUser().getName());
                    donorInfo.put("email", d.getUser().getEmail());
                    donorInfo.put("mobile", d.getUser().getMobile());
                    donorInfo.put("bloodGroup", d.getBloodGroup());
                    donorInfo.put("units", d.getUnits());
                    donorInfo.put("city", d.getCity());
                    donorInfo.put("gender", d.getGender());
                    donorInfo.put("age", d.getAge());
                    donorInfo.put("priorityScore", donorPriorityScoreService.calculatePriorityScore(d));
                    donorInfo.put("isPreviousDonor", d.getUnits() > 0);  // Priority flag
                    donorInfo.put("lastDonationDate", d.getLastDonationDate());
                    donorInfo.put("profilePhoto", d.getUser().getProfilePhoto());
                    return donorInfo;
                })
                .sorted((a, b) -> {
                    // Prioritize previous donors
                    Boolean prevA = (Boolean) a.get("isPreviousDonor");
                    Boolean prevB = (Boolean) b.get("isPreviousDonor");
                    if (prevB && !prevA) return 1;
                    if (prevA && !prevB) return -1;
                    return 0;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("donors", matchedDonors);
            response.put("totalMatches", matchedDonors.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching donors: " + e.getMessage());
        }
    }

    /* ===== FULFILL BLOOD REQUEST ===== */
    @PostMapping("/fulfill/{requestId}")
    public ResponseEntity<?> fulfillRequest(
            @PathVariable Long requestId,
            @RequestParam(required = false) String notes) {
        try {
            BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                return ResponseEntity.badRequest().body("Request not found");
            }
            
            request.setStatus("COMPLETED");
            request.setFulfilledDate(LocalDateTime.now());
            request = bloodRequestRepository.save(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", request);
            response.put("message", "Request completed successfully!");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fulfilling request: " + e.getMessage());
        }
    }

    /* ===== APPROVE BLOOD REQUEST WITH NOTIFICATION ===== */
    @PostMapping("/approve/{requestId}")
    public ResponseEntity<?> approveBloodRequest(
            @PathVariable Long requestId,
            HttpSession session,
            @RequestParam(required = false) String expectedDeliveryTime,
            @RequestParam(required = false) String deliveryLocation,
            @RequestParam(required = false) String deliveryInstructions) {
        try {
            if (session.getAttribute("admin") == null) {
                return ResponseEntity.status(401).body("Unauthorized");
            }

            BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                return ResponseEntity.badRequest().body("Request not found");
            }
            if ("RESERVED".equalsIgnoreCase(request.getStatus())
                    || "FULFILLED".equalsIgnoreCase(request.getStatus())
                    || "COMPLETED".equalsIgnoreCase(request.getStatus())) {
                return ResponseEntity.badRequest().body("Request has already been processed");
            }

            String resolvedExpectedDeliveryTime = expectedDeliveryTime == null || expectedDeliveryTime.isBlank()
                ? "Within 2-3 hours"
                : expectedDeliveryTime.trim();
            String resolvedDeliveryLocation = deliveryLocation == null || deliveryLocation.isBlank()
                ? (request.getHospital() == null || request.getHospital().isBlank() ? "Hospital Counter" : request.getHospital())
                : deliveryLocation.trim();

            Map<String, Object> reservation = null;
            String approvalMessage = "Blood request approved successfully.";

            try {
                reservation = bloodStockService.reserveCompatibleUnits(
                    request.getHospital(),
                    request.getBloodGroup(),
                    request.getUnitsRequired());
                request.setStatus("RESERVED");
                approvalMessage = "Blood request approved and compatible units reserved.";
            } catch (IllegalArgumentException reserveError) {
                // Fallback: keep the request approved and visible for donor matching even if local stock is unavailable.
                request.setStatus("OPEN");
                approvalMessage = "Blood request approved. Compatible stock was not reserved, so the request is now open for donor matching.";
            }

            request.setVerified(true);
            request.setExpectedDeliveryTime(resolvedExpectedDeliveryTime);
            request.setDeliveryLocation(resolvedDeliveryLocation);
            request.setDeliveryInstructions(deliveryInstructions == null || deliveryInstructions.isBlank()
                ? null
                : deliveryInstructions.trim());
            request.setApprovedDate(LocalDateTime.now());
            
            request = bloodRequestRepository.save(request);

            List<Map<String, Object>> matchedDonors = checkerService.matchAndNotify(
                    request.getBloodGroup(),
                    request.getCity(),
                    request.getUnitsRequired(),
                    10);
            request.setMatchedDonorsCount(matchedDonors.size());
            request = bloodRequestRepository.save(request);

            int emergencyBroadcastCount = 0;
            if (matchedDonors.isEmpty() && !"RESERVED".equalsIgnoreCase(request.getStatus())) {
                emergencyBroadcastCount = checkerService.sendEmergencyNotificationToAll(
                        request.getBloodGroup(),
                        request.getCity(),
                        request.getPublicId()).size();
            }
            
            // Send delivery notification only when stock was actually reserved.
            if (request.getUser() != null) {
                if ("RESERVED".equalsIgnoreCase(request.getStatus())) {
                    notificationService.createBloodDeliveryNotification(
                        request.getUser(),
                        request.getBloodGroup(),
                        request.getUnitsRequired(),
                        request.getHospital(),
                        request.getDeliveryLocation(),
                        request.getExpectedDeliveryTime()
                    );
                } else {
                    notificationService.createNotification(
                        request.getUser(),
                        "Blood Request Approved",
                        "Your blood request has been approved and published for donor matching.",
                        "BLOOD_REQUEST_APPROVED"
                    );
                }

                try {
                    emailService.sendBloodRequestApprovalEmail(
                        request.getUser().getEmail(),
                        request.getUser().getName(),
                        request
                    );
                } catch (Exception mailError) {
                    System.out.println("Receiver approval email failed: " + mailError.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("request", request);
            response.put("reservation", reservation);
            response.put("matchedDonors", matchedDonors.size());
            response.put("emergencyBroadcastCount", emergencyBroadcastCount);
            response.put("message", approvalMessage);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error approving request: " + e.getMessage());
        }
    }

    @PostMapping("/reject/{requestId}")
    public ResponseEntity<?> rejectBloodRequest(@PathVariable Long requestId, HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            BloodRequest request = bloodRequestRepository.findById(requestId).orElse(null);
            if (request == null) {
                return ResponseEntity.badRequest().body("Request not found");
            }

            request.setStatus("REJECTED");
            request.setVerified(false);
            bloodRequestRepository.save(request);

            if (request.getUser() != null) {
                notificationService.createNotification(
                    request.getUser(),
                    "Blood Request Update",
                    "Your request " + request.getPublicId() + " could not be approved by the hospital.",
                    "BLOOD_REQUEST");
            }

            return ResponseEntity.ok(Map.of("success", true, "request", request, "message", "Blood request rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error rejecting request: " + e.getMessage());
        }
    }

    /* ===== GET USER'S BLOOD REQUESTS ===== */
    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            List<BloodRequest> requests = bloodRequestRepository.findByUser(user);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching requests");
        }
    }

    /* ===== GET CRITICAL REQUESTS FOR ADMIN ===== */
    @GetMapping("/admin/alerts")
    public ResponseEntity<?> getAdminAlerts() {
        try {
            // Get all critical and high urgency open requests
            List<BloodRequest> critical = bloodRequestRepository
                .findByUrgencyAndStatusOrderByCreatedDateDesc("CRITICAL", "PENDING_APPROVAL");
            List<BloodRequest> high = bloodRequestRepository
                .findByUrgencyAndStatusOrderByCreatedDateDesc("HIGH", "PENDING_APPROVAL");
            
            List<BloodRequest> alerts = new java.util.ArrayList<>();
            alerts.addAll(critical);
            alerts.addAll(high);
            
            Map<String, Object> response = new HashMap<>();
            response.put("criticalCount", critical.size());
            response.put("highCount", high.size());
            response.put("totalAlerts", alerts.size());
            response.put("alerts", alerts);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching alerts");
        }
    }

    /* ===== CANCEL BLOOD REQUEST ===== */
    @PostMapping("/cancel/{requestId}")
    public ResponseEntity<?> cancelRequest(@PathVariable Long requestId) {
        try {
            BloodRequest request = bloodRequestRepository.findById(requestId)
                .orElse(null);
            
            if (request == null) {
                return ResponseEntity.badRequest().body("Request not found");
            }
            
            request.setStatus("CANCELLED");
            request = bloodRequestRepository.save(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Request cancelled");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error cancelling request");
        }
    }

    /* ===== HELPER METHOD: URGENCY ORDER ===== */
    private int getUrgencyOrder(String urgency) {
        if ("CRITICAL".equals(urgency)) return 4;
        if ("HIGH".equals(urgency)) return 3;
        if ("MEDIUM".equals(urgency)) return 2;
        return 1;  // LOW
    }
}
