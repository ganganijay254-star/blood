package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.Admin;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.entity.Certificate;
import com.bloodcare.bloodcare.entity.Hospital;
import com.bloodcare.bloodcare.entity.BloodBank;
import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.repository.AdminRepository;
import com.bloodcare.bloodcare.repository.UserRepository;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;
import com.bloodcare.bloodcare.repository.CertificateRepository;
import com.bloodcare.bloodcare.repository.BloodRequestRepository;
import com.bloodcare.bloodcare.repository.BloodRequestResponseRepository;
import com.bloodcare.bloodcare.repository.ChatbotMessageRepository;
import com.bloodcare.bloodcare.repository.NotificationRepository;
import com.bloodcare.bloodcare.repository.RewardRepository;
import com.bloodcare.bloodcare.repository.HospitalRepository;
import com.bloodcare.bloodcare.repository.BloodBankRepository;
import com.bloodcare.bloodcare.repository.BloodStockRepository;
import com.bloodcare.bloodcare.service.BloodStockService;
import com.bloodcare.bloodcare.service.EmailService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final List<String> BLOOD_GROUP_ORDER = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final Map<String, Object> ADMIN_SETTINGS = new HashMap<>();
    private static final Map<String, Boolean> USER_PANEL_CONTROLS = new HashMap<>();

    static {
        ADMIN_SETTINGS.put("pointsPerUnit", 100);
        ADMIN_SETTINGS.put("bonusPerDonation", 200);
        ADMIN_SETTINGS.put("agePointsMultiplier", 5);
        ADMIN_SETTINGS.put("minAge", 18);
        ADMIN_SETTINGS.put("maxAge", 65);
        ADMIN_SETTINGS.put("minWeight", 50);
        ADMIN_SETTINGS.put("maxUnitsPerDonation", 2);
        ADMIN_SETTINGS.put("leaderboardSize", 10);

        USER_PANEL_CONTROLS.put("showDonorProfile", true);
        USER_PANEL_CONTROLS.put("showBloodRequest", true);
        USER_PANEL_CONTROLS.put("showLeaderboard", true);
        USER_PANEL_CONTROLS.put("showMedicine", true);
        USER_PANEL_CONTROLS.put("showCertificates", true);
        USER_PANEL_CONTROLS.put("showRewards", true);
        USER_PANEL_CONTROLS.put("showChatbot", true);
        USER_PANEL_CONTROLS.put("enableDonations", true);
        USER_PANEL_CONTROLS.put("enableRequests", true);
        USER_PANEL_CONTROLS.put("maintenanceMode", false);
    }

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private BloodRequestResponseRepository bloodRequestResponseRepository;

    @Autowired
    private ChatbotMessageRepository chatbotMessageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private BloodStockRepository bloodStockRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BloodStockService bloodStockService;

    /* ================= ADMIN LOGIN ================= */
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> req,
                                        HttpSession session) {

        String email = req.get("email");
        String password = req.get("password");

        Admin admin = adminRepository.findByEmail(email);

        if (admin == null ||
            !passwordEncoder.matches(password, admin.getPassword())) {

            return ResponseEntity.status(401)
                    .body("Invalid admin credentials");
        }

        admin.setPassword(null);
        session.setAttribute("admin", admin);

        return ResponseEntity.ok(admin);
    }

    /* ================= CHECK SESSION ================= */
    @GetMapping("/check-session")
    public ResponseEntity<?> checkAdminSession(HttpSession session) {

        Admin admin = (Admin) session.getAttribute("admin");

        if (admin == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("admin", admin);
        return ResponseEntity.ok(response);
    }

    /* ================= LOGOUT ================= */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.removeAttribute("admin");
        return ResponseEntity.ok("Logged out");
    }

    /* ================= USERS ================= */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {

        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        List<User> users = userRepository.findAll();
        users.forEach(u -> u.setPassword(null));

        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<?> updateUserBlockStatus(@PathVariable Long id,
                                                   @RequestParam boolean blocked,
                                                   HttpSession session) {

        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        User user = userRepository.findById(id).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User not found");

        user.setBlocked(blocked);
        user.setBlockedByAdmin(blocked);
        userRepository.save(user);

        return ResponseEntity.ok(blocked ? "User blocked successfully" : "User unblocked successfully");
    }

    @Transactional
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        List<VisitRequest> userVisits = visitRequestRepository.findByUser(user);
        if (!userVisits.isEmpty()) {
            certificateRepository.deleteByVisitRequestIn(userVisits);
        }
        visitRequestRepository.deleteByUser(user);

        List<com.bloodcare.bloodcare.entity.BloodRequest> userRequests = bloodRequestRepository.findByUser(user);
        if (!userRequests.isEmpty()) {
            bloodRequestResponseRepository.deleteByBloodRequestIn(userRequests);
        }
        bloodRequestRepository.deleteByUser(user);

        rewardRepository.deleteByUser(user);
        notificationRepository.deleteByUser(user);
        chatbotMessageRepository.deleteByUser(user);

        Donor donor = donorRepository.findByUser(user);
        if (donor != null) {
            certificateRepository.deleteByDonor(donor);
            bloodRequestResponseRepository.deleteByDonor(donor);

            List<com.bloodcare.bloodcare.entity.BloodRequest> confirmedByDonor = bloodRequestRepository.findByConfirmedDonor(donor);
            for (com.bloodcare.bloodcare.entity.BloodRequest request : confirmedByDonor) {
                request.setConfirmedDonor(null);
            }
            if (!confirmedByDonor.isEmpty()) {
                bloodRequestRepository.saveAll(confirmedByDonor);
            }

            donorRepository.delete(donor);
        }

        userRepository.delete(user);
        return ResponseEntity.ok("User deleted successfully");
    }

    /* ================= RECEIVER REQUESTS FOR ADMIN ================= */
    @GetMapping("/receiver-requests")
    public ResponseEntity<?> getReceiverRequests(HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        List<com.bloodcare.bloodcare.entity.BloodRequest> requests = bloodRequestRepository.findAll();

        List<Map<String, Object>> mapped = requests.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("user", r.getUser() != null ? new HashMap<String, Object>() {{
                put("id", r.getUser().getId());
                put("name", r.getUser().getName());
                put("email", r.getUser().getEmail());
            }} : null);
            m.put("hospitalName", r.getHospital());
            m.put("units", r.getUnitsRequired());
            m.put("bloodUrgency", r.getUrgency());
            m.put("status", r.getStatus());
            m.put("matchedDonors", r.getMatchedDonorsCount());
            m.put("donorsResponded", r.getDonorsRespondedCount());
            m.put("publicId", r.getPublicId());
            m.put("requestType", "RECEIVER");
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(mapped);
    }

    /* ================= DONORS ================= */
    @GetMapping("/donors")
    public ResponseEntity<?> getAllDonors(HttpSession session) {

        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(donorRepository.findAll());
    }

    /* ================= HOSPITALS ================= */
    @GetMapping("/hospitals")
    public ResponseEntity<?> getHospitals(HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(hospitalRepository.findAll());
    }

    @PostMapping("/hospital")
    public ResponseEntity<?> addHospital(@RequestBody Hospital hospital, HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        if (hospital.getName() != null) hospital.setName(hospital.getName().trim());
        if (hospital.getAddress() != null) hospital.setAddress(hospital.getAddress().trim());
        if (hospital.getContact() != null) hospital.setContact(hospital.getContact().trim());

        return ResponseEntity.ok(hospitalRepository.save(hospital));
    }

    /* ================= BLOOD BANKS ================= */
    @GetMapping("/blood-banks")
    public ResponseEntity<?> getBloodBanks(HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(bloodBankRepository.findAll());
    }

    @PostMapping("/blood-bank")
    public ResponseEntity<?> addBloodBank(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        String hospitalIdValue = String.valueOf(payload.getOrDefault("hospitalId", "")).trim();
        Long hospitalId = hospitalIdValue.isEmpty() ? null : Long.valueOf(hospitalIdValue);
        Hospital hospital = hospitalId == null ? null : hospitalRepository.findById(hospitalId).orElse(null);
        if (hospital == null) {
            return ResponseEntity.badRequest().body("Hospital not found");
        }

        BloodBank bank = new BloodBank();
        bank.setName(String.valueOf(payload.getOrDefault("name", "")).trim());
        bank.setHospital(hospital);
        bank.setLocation(hospital.getAddress() == null || hospital.getAddress().isBlank() ? hospital.getName() : hospital.getAddress());
        bank.setBloodGroup(String.valueOf(payload.getOrDefault("bloodGroup", "GENERAL")).trim().toUpperCase());
        Object quantityValue = payload.get("quantity");
        bank.setQuantity(quantityValue == null ? 0 : Integer.parseInt(String.valueOf(quantityValue)));

        return ResponseEntity.ok(bloodBankRepository.save(bank));
    }

    /* ================= BLOOD STOCK ================= */
    @GetMapping("/blood-stocks")
    public ResponseEntity<?> getBloodStocks(HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        return ResponseEntity.ok(bloodStockRepository.findAll());
    }

    @PostMapping("/blood-stock")
    public ResponseEntity<?> saveBloodStock(@RequestBody Map<String, Object> payload, HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        String bloodBankIdValue = String.valueOf(payload.getOrDefault("bloodBankId", "")).trim();
        Long bloodBankId = bloodBankIdValue.isEmpty() ? null : Long.valueOf(bloodBankIdValue);
        if (bloodBankId == null) {
            return ResponseEntity.badRequest().body("Blood bank is required");
        }

        BloodBank bank = bloodBankRepository.findById(bloodBankId).orElse(null);
        if (bank == null) {
            return ResponseEntity.badRequest().body("Blood bank not found");
        }

        String bloodGroup = String.valueOf(payload.getOrDefault("bloodGroup", "")).trim().toUpperCase();
        int units = Integer.parseInt(String.valueOf(payload.getOrDefault("units", 0)));

        BloodStock existing = bloodStockRepository.findByBloodBankBloodBankIDAndBloodGroupIgnoreCase(bloodBankId, bloodGroup);
        BloodStock stock = existing != null ? existing : new BloodStock();
        stock.setBloodBank(bank);
        stock.setHospital(bank.getHospital());
        stock.setBloodGroup(bloodGroup);
        stock.setUnitsAvailable(Math.max(0, units));
        stock.setLastUpdated(LocalDateTime.now());
        if (stock.getReorderLevel() <= 0) {
            stock.setReorderLevel(1);
        }

        bloodStockRepository.save(stock);
        return ResponseEntity.ok(Map.of("message", "Stock Saved", "stock", stock));
    }

    /* ================= VISITS ================= */
    @GetMapping("/visits")
    public ResponseEntity<?> getAllVisits(HttpSession session) {

        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        List<Map<String, Object>> visits = visitRequestRepository.findAll()
                .stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getId());
                    map.put("user", v.getUser() != null ? new HashMap<String, Object>() {{
                        put("id", v.getUser().getId());
                        put("name", v.getUser().getName());
                        put("email", v.getUser().getEmail());
                    }} : null);
                    map.put("hospitalName", v.getHospitalName());
                    map.put("units", v.getUnits());
                    map.put("status", v.getStatus());
                    map.put("requestDate", v.getRequestDate());
                    map.put("requestType", v.getRequestType());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(visits);
    }

    /* ================= APPROVE / REJECT VISIT ================= */
    @PutMapping("/visit/{id}")
    public ResponseEntity<?> updateVisitStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpSession session) {

        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        VisitRequest visit = visitRequestRepository.findById(id).orElse(null);

        if (visit == null)
            return ResponseEntity.badRequest().body("Visit not found");

        // Only allow update if PENDING
        if (!"PENDING".equalsIgnoreCase(visit.getStatus()))
            return ResponseEntity.badRequest()
                    .body("Visit already processed");

        // Only allow APPROVED or REJECTED
        if (!"APPROVED".equalsIgnoreCase(status)
                && !"REJECTED".equalsIgnoreCase(status)) {

            return ResponseEntity.badRequest()
                    .body("Invalid status value");
        }

        visit.setStatus(status.toUpperCase());
        visitRequestRepository.save(visit);

        String email = visit.getUser().getEmail();
        String name = visit.getUser().getName();
        String hospital = visit.getHospitalName();

        try {

            if ("APPROVED".equalsIgnoreCase(status)) {
                System.out.println("Sending approval email to: " + email);
                emailService.sendVisitApprovalEmail(email, name, hospital);

                // Create certificate for donor visits. Older rows may not have requestType set.
                if (!"RECEIVER".equalsIgnoreCase(visit.getRequestType())) {
                    createDonationCertificate(visit);
                }
            }

            if ("REJECTED".equalsIgnoreCase(status)) {
                System.out.println("Sending rejection email to: " + email);
                emailService.sendVisitRejectionEmail(email, name, hospital);
            }

        } catch (Exception e) {
            System.out.println("Email sending failed: " + e.getMessage());
            // Status update ho chuka hai, mail fail hua to bhi API fail nahi hogi
        }

        return ResponseEntity.ok("Visit status updated successfully");
    }

    /* ================= CREATE DONATION CERTIFICATE ================= */
    private void createDonationCertificate(VisitRequest visit) {
        try {
            if (certificateRepository.existsByVisitRequest(visit)) {
                System.out.println("Certificate already exists for visit: " + visit.getId());
                return;
            }

            // Find donor by user
            Donor donor = donorRepository.findByUser(visit.getUser());
            
            if (donor == null) {
                System.out.println("Donor not found for user: " + visit.getUser().getId());
                return;
            }

            Certificate certificate = new Certificate();
            certificate.setDonor(donor);
            certificate.setVisitRequest(visit);
            
            // Generate unique certificate number
            String certificateNumber = "CERT-" + visit.getId() + "-" + System.currentTimeMillis();
            certificate.setCertificateNumber(certificateNumber);
            certificate.setHospitalName(visit.getHospitalName());
            certificate.setUnits(visit.getUnits());
            certificate.setDonationDate(
                    visit.getVisitDate() != null
                            ? visit.getVisitDate()
                            : (visit.getRequestDate() != null ? visit.getRequestDate() : LocalDate.now()));
            certificate.setCreatedDate(LocalDateTime.now());
            
            // Store relative verification path; public base is resolved when rendering certificate page
            String qrCodeData = "/verify-certificate?cert=" + certificateNumber;
            certificate.setQrCode(qrCodeData);
            
            certificateRepository.save(certificate);
            System.out.println("Certificate created: " + certificateNumber);
            
        } catch (Exception e) {
            System.out.println("Certificate creation failed: " + e.getMessage());
        }
    }

    /* ================= NORMAL USERS ================= */
    @GetMapping("/normal-users")
    public ResponseEntity<?> getNormalUsers(HttpSession session) {

        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).build();

        List<User> allUsers = userRepository.findAll();

        List<Long> donorUserIds = donorRepository.findAll()
                .stream()
                .map(d -> d.getUser().getId())
                .collect(Collectors.toList());

        List<Long> visitUserIds = visitRequestRepository.findAll()
                .stream()
                .map(v -> v.getUser().getId())
                .distinct()
                .collect(Collectors.toList());

        List<User> normalUsers = allUsers.stream()
                .filter(u -> !donorUserIds.contains(u.getId())
                        && !visitUserIds.contains(u.getId()))
                .collect(Collectors.toList());

        normalUsers.forEach(u -> u.setPassword(null));

        return ResponseEntity.ok(normalUsers);
    }

    /* ================= CREATE ADMIN ================= */
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin() {

        if (adminRepository.findByEmail("admin@bloodcare.com") != null)
            return ResponseEntity.ok("Admin already exists");

        Admin admin = new Admin();
        admin.setName("Admin");
        admin.setEmail("admin@bloodcare.com");
        admin.setPassword(passwordEncoder.encode("Admin@123"));

        adminRepository.save(admin);

        return ResponseEntity.ok("Admin created successfully");
    }

    /* ================= ADMIN SETTINGS ================= */
    @GetMapping("/settings")
    public ResponseEntity<?> getAdminSettings(HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(new HashMap<>(ADMIN_SETTINGS));
    }

    /* ================= UPDATE ADMIN SETTINGS ================= */
    @PostMapping("/settings")
    public ResponseEntity<?> updateAdminSettings(
            @RequestBody Map<String, Object> settings,
            HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");
        // Validate and normalize expected keys so the frontend receives consistent data
        Map<String, Object> normalized = new HashMap<>();
        try {
            Integer pointsPerUnit = toInt(settings.get("pointsPerUnit"), 100);
            Integer bonusPerDonation = toInt(settings.get("bonusPerDonation"), 200);
            Integer agePointsMultiplier = toInt(settings.get("agePointsMultiplier"), 5);
            Integer minAge = toInt(settings.get("minAge"), 18);
            Integer maxAge = toInt(settings.get("maxAge"), 65);
            Integer minWeight = toInt(settings.get("minWeight"), 50);

            normalized.put("pointsPerUnit", pointsPerUnit);
            normalized.put("bonusPerDonation", bonusPerDonation);
            normalized.put("agePointsMultiplier", agePointsMultiplier);
            normalized.put("minAge", minAge);
            normalized.put("maxAge", maxAge);
            normalized.put("minWeight", minWeight);
            normalized.put("maxUnitsPerDonation", toInt(settings.get("maxUnitsPerDonation"), 2));
            normalized.put("leaderboardSize", toInt(settings.get("leaderboardSize"), 10));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid settings payload");
        }

        ADMIN_SETTINGS.putAll(normalized);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Settings updated successfully");
        response.put("settings", normalized);

        return ResponseEntity.ok(response);
    }

    // Helper to convert flexible number types to Integer with default
    private Integer toInt(Object o, Integer defaultVal) {
        if (o == null) return defaultVal;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /* ================= CONTROL USER PANEL ================= */
    @GetMapping("/controls")
    public ResponseEntity<?> getUserPanelControls(HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");
        return ResponseEntity.ok(new HashMap<>(USER_PANEL_CONTROLS));
    }

    /* ================= UPDATE USER PANEL CONTROLS ================= */
    @PostMapping("/controls")
    public ResponseEntity<?> updateUserPanelControls(
            @RequestBody Map<String, Boolean> controls,
            HttpSession session) {
        if (session.getAttribute("admin") == null)
            return ResponseEntity.status(401).body("Unauthorized");

        USER_PANEL_CONTROLS.put("showDonorProfile", controls.getOrDefault("showDonorProfile", true));
        USER_PANEL_CONTROLS.put("showBloodRequest", controls.getOrDefault("showBloodRequest", true));
        USER_PANEL_CONTROLS.put("showLeaderboard", controls.getOrDefault("showLeaderboard", true));
        USER_PANEL_CONTROLS.put("showMedicine", controls.getOrDefault("showMedicine", true));
        USER_PANEL_CONTROLS.put("showCertificates", controls.getOrDefault("showCertificates", true));
        USER_PANEL_CONTROLS.put("showRewards", controls.getOrDefault("showRewards", true));
        USER_PANEL_CONTROLS.put("showChatbot", controls.getOrDefault("showChatbot", true));
        USER_PANEL_CONTROLS.put("enableDonations", controls.getOrDefault("enableDonations", true));
        USER_PANEL_CONTROLS.put("enableRequests", controls.getOrDefault("enableRequests", true));
        USER_PANEL_CONTROLS.put("maintenanceMode", controls.getOrDefault("maintenanceMode", false));

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User panel controls updated successfully");
        response.put("controls", new HashMap<>(USER_PANEL_CONTROLS));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public-config")
    public ResponseEntity<?> getPublicConfig() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("settings", new HashMap<>(ADMIN_SETTINGS));
        payload.put("controls", new HashMap<>(USER_PANEL_CONTROLS));
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/public-overview")
    public ResponseEntity<?> getPublicOverview() {
        List<Donor> donors = donorRepository.findAll();
        List<VisitRequest> visits = visitRequestRepository.findAll();
        List<com.bloodcare.bloodcare.entity.BloodRequest> receiverRequests = bloodRequestRepository.findAll();
        List<BloodStock> stocks = bloodStockRepository.findAll();

        long approvedDonations = visits.stream()
                .filter(this::isApprovedDonorVisit)
                .count();
        int donatedUnits = visits.stream()
                .filter(this::isApprovedDonorVisit)
                .mapToInt(VisitRequest::getUnits)
                .sum();
        int totalAvailableUnits = stocks.stream()
                .mapToInt(BloodStock::getUnitsAvailable)
                .sum();
        long activeRequests = receiverRequests.stream()
                .filter(this::isPublicLiveRequest)
                .count();

        List<Map<String, Object>> criticalRequests = receiverRequests.stream()
                .filter(this::isPublicLiveRequest)
                .sorted(Comparator
                        .comparingInt((com.bloodcare.bloodcare.entity.BloodRequest r) -> urgencyRank(r.getUrgency())).reversed()
                        .thenComparing(com.bloodcare.bloodcare.entity.BloodRequest::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::toRequestPulseCard)
                .collect(Collectors.toList());

        Map<String, Integer> stockTotals = new LinkedHashMap<>();
        for (String group : BLOOD_GROUP_ORDER) {
            stockTotals.put(group, 0);
        }
        for (BloodStock stock : stocks) {
            String group = normalizeBloodGroup(stock.getBloodGroup());
            stockTotals.put(group, stockTotals.getOrDefault(group, 0) + Math.max(stock.getUnitsAvailable(), 0));
        }
        List<Map<String, Object>> stockGroups = stockTotals.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("bloodGroup", entry.getKey());
                    row.put("units", entry.getValue());
                    row.put("tone", entry.getValue() <= 2 ? "critical" : entry.getValue() <= 8 ? "watch" : "stable");
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, List<com.bloodcare.bloodcare.entity.BloodRequest>> cityBuckets = receiverRequests.stream()
                .filter(this::isPublicLiveRequest)
                .collect(Collectors.groupingBy(request -> {
                    String city = request.getCity();
                    return city == null || city.isBlank() ? "Unspecified" : city.trim();
                }));
        List<Map<String, Object>> cityPulse = cityBuckets.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(4)
                .map(entry -> {
                    long criticalCount = entry.getValue().stream()
                            .filter(req -> urgencyRank(req.getUrgency()) >= urgencyRank("HIGH"))
                            .count();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("city", entry.getKey());
                    row.put("requestCount", entry.getValue().size());
                    row.put("criticalCount", criticalCount);
                    row.put("tone", criticalCount > 0 ? "hot" : "steady");
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("donorCount", donors.size());
        stats.put("approvedDonations", approvedDonations);
        stats.put("donatedUnits", donatedUnits);
        stats.put("availableUnits", totalAvailableUnits);
        stats.put("activeRequests", activeRequests);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stats", stats);
        payload.put("criticalRequests", criticalRequests);
        payload.put("stockGroups", stockGroups);
        payload.put("cityPulse", cityPulse);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/dashboard-insights")
    public ResponseEntity<?> getDashboardInsights(HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        List<Donor> donors = donorRepository.findAll();
        List<VisitRequest> visits = visitRequestRepository.findAll();
        List<com.bloodcare.bloodcare.entity.BloodRequest> receiverRequests = bloodRequestRepository.findAll();
        List<BloodStock> stocks = bloodStockRepository.findAll();

        List<com.bloodcare.bloodcare.entity.BloodRequest> liveRequests = receiverRequests.stream()
                .filter(this::isPublicLiveRequest)
                .collect(Collectors.toList());

        int totalAvailableUnits = stocks.stream().mapToInt(BloodStock::getUnitsAvailable).sum();
        long criticalPendingCount = receiverRequests.stream()
                .filter(request -> isPendingStatus(request.getStatus()) && urgencyRank(request.getUrgency()) >= urgencyRank("CRITICAL"))
                .count();
        long liveRequestCount = liveRequests.size();
        long readyDonors = donors.stream().filter(Donor::isAvailable).count();
        long lowStockCount = stocks.stream().filter(this::isLowStock).count();
        long assignedRequests = liveRequests.stream()
                .filter(request -> "DONOR_ASSIGNED".equalsIgnoreCase(safeUpper(request.getStatus())))
                .count();

        int totalMatched = liveRequests.stream().mapToInt(com.bloodcare.bloodcare.entity.BloodRequest::getMatchedDonorsCount).sum();
        int totalResponded = liveRequests.stream().mapToInt(com.bloodcare.bloodcare.entity.BloodRequest::getDonorsRespondedCount).sum();
        int responseRate = totalMatched > 0 ? (int) Math.round((totalResponded * 100.0) / totalMatched) : 0;

        Map<String, Object> commandStats = new LinkedHashMap<>();
        commandStats.put("criticalPendingCount", criticalPendingCount);
        commandStats.put("liveRequestCount", liveRequestCount);
        commandStats.put("readyDonors", readyDonors);
        commandStats.put("lowStockCount", lowStockCount);
        commandStats.put("responseRate", responseRate);
        commandStats.put("assignedRequests", assignedRequests);
        commandStats.put("availableUnits", totalAvailableUnits);
        commandStats.put("approvedDonationCount", visits.stream().filter(this::isApprovedDonorVisit).count());

        List<Map<String, Object>> urgentQueue = liveRequests.stream()
                .sorted(Comparator
                        .comparingInt((com.bloodcare.bloodcare.entity.BloodRequest r) -> urgencyRank(r.getUrgency())).reversed()
                        .thenComparing(com.bloodcare.bloodcare.entity.BloodRequest::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(request -> {
                    Map<String, Object> row = toRequestPulseCard(request);
                    row.put("matchedDonors", request.getMatchedDonorsCount());
                    row.put("donorsResponded", request.getDonorsRespondedCount());
                    row.put("needsAction", isPendingStatus(request.getStatus()));
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, Integer> demandByGroup = new LinkedHashMap<>();
        Map<String, Integer> availableByGroup = new LinkedHashMap<>();
        for (String group : BLOOD_GROUP_ORDER) {
            demandByGroup.put(group, 0);
            availableByGroup.put(group, 0);
        }
        for (com.bloodcare.bloodcare.entity.BloodRequest request : liveRequests) {
            String group = normalizeBloodGroup(request.getBloodGroup());
            demandByGroup.put(group, demandByGroup.getOrDefault(group, 0) + Math.max(request.getUnitsRequired(), 0));
        }
        for (BloodStock stock : stocks) {
            String group = normalizeBloodGroup(stock.getBloodGroup());
            availableByGroup.put(group, availableByGroup.getOrDefault(group, 0) + Math.max(stock.getUnitsAvailable(), 0));
        }
        List<Map<String, Object>> stockPressure = BLOOD_GROUP_ORDER.stream()
                .map(group -> {
                    int available = availableByGroup.getOrDefault(group, 0);
                    int demand = demandByGroup.getOrDefault(group, 0);
                    int gap = available - demand;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("bloodGroup", group);
                    row.put("availableUnits", available);
                    row.put("demandUnits", demand);
                    row.put("tone", gap < 0 || (available == 0 && demand > 0) ? "critical"
                            : gap <= 2 ? "watch" : "stable");
                    return row;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> shortageMap = stocks.stream()
                .filter(this::isLowStock)
                .sorted(Comparator
                        .comparingInt((BloodStock stock) -> stock.getUnitsAvailable() - stock.getReorderLevel())
                        .thenComparing(stock -> stock.getLastUpdated(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .map(stock -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("hospital", stock.getHospital() != null ? stock.getHospital().getName() : "Unknown hospital");
                    row.put("bloodGroup", normalizeBloodGroup(stock.getBloodGroup()));
                    row.put("unitsAvailable", stock.getUnitsAvailable());
                    row.put("reorderLevel", stock.getReorderLevel());
                    row.put("tone", stock.getUnitsAvailable() == 0 ? "critical" : "watch");
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, List<com.bloodcare.bloodcare.entity.BloodRequest>> demandByCity = liveRequests.stream()
                .collect(Collectors.groupingBy(request -> {
                    String city = request.getCity();
                    return city == null || city.isBlank() ? "Unspecified" : city.trim();
                }));
        List<Map<String, Object>> cityDemand = demandByCity.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(6)
                .map(entry -> {
                    int critical = (int) entry.getValue().stream()
                            .filter(request -> urgencyRank(request.getUrgency()) >= urgencyRank("HIGH"))
                            .count();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("city", entry.getKey());
                    row.put("requests", entry.getValue().size());
                    row.put("critical", critical);
                    row.put("tone", critical > 0 ? "critical" : "steady");
                    return row;
                })
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commandStats", commandStats);
        payload.put("urgentQueue", urgentQueue);
        payload.put("stockPressure", stockPressure);
        payload.put("shortageMap", shortageMap);
        payload.put("cityDemand", cityDemand);
        return ResponseEntity.ok(payload);
    }

    public static int getSettingInt(String key, int defaultValue) {
        Object value = ADMIN_SETTINGS.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static boolean isControlEnabled(String key, boolean defaultValue) {
        Boolean value = USER_PANEL_CONTROLS.get(key);
        return value == null ? defaultValue : value;
    }

    @GetMapping("/blood-stock-overview")
    public ResponseEntity<?> bloodStockOverview(HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        return ResponseEntity.ok(bloodStockService.overview());
    }

    private boolean isApprovedDonorVisit(VisitRequest visit) {
        if (visit == null) return false;
        boolean approved = "APPROVED".equalsIgnoreCase(safeUpper(visit.getStatus()));
        boolean receiverType = "RECEIVER".equalsIgnoreCase(safeUpper(visit.getRequestType()));
        return approved && !receiverType;
    }

    private boolean isPublicLiveRequest(com.bloodcare.bloodcare.entity.BloodRequest request) {
        if (request == null) return false;
        String status = safeUpper(request.getStatus());
        return isPendingStatus(status)
                || "OPEN".equals(status)
                || "RESERVED".equals(status)
                || "IN_PROGRESS".equals(status)
                || "DONOR_ASSIGNED".equals(status)
                || "BLOOD_BANK_AVAILABLE".equals(status);
    }

    private boolean isPendingStatus(String status) {
        String normalized = safeUpper(status);
        return "PENDING".equals(normalized)
                || "SUBMITTED".equals(normalized)
                || "PENDING_APPROVAL".equals(normalized);
    }

    private boolean isLowStock(BloodStock stock) {
        if (stock == null) return false;
        return stock.getUnitsAvailable() <= Math.max(stock.getReorderLevel(), 1);
    }

    private int urgencyRank(String urgency) {
        return switch (safeUpper(urgency)) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private String normalizeBloodGroup(String value) {
        String normalized = safeUpper(value);
        return normalized.isBlank() ? "UNKNOWN" : normalized;
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private Map<String, Object> toRequestPulseCard(com.bloodcare.bloodcare.entity.BloodRequest request) {
        LocalDateTime createdDate = request.getCreatedDate();
        long waitMinutes = createdDate == null ? 0 : Math.max(0, Duration.between(createdDate, LocalDateTime.now()).toMinutes());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", request.getId());
        row.put("publicId", request.getPublicId() == null || request.getPublicId().isBlank()
                ? "REQ-" + request.getId()
                : request.getPublicId());
        row.put("hospital", request.getHospital() == null || request.getHospital().isBlank() ? "Unknown hospital" : request.getHospital());
        row.put("city", request.getCity() == null || request.getCity().isBlank() ? "Unknown city" : request.getCity());
        row.put("bloodGroup", normalizeBloodGroup(request.getBloodGroup()));
        row.put("unitsRequired", request.getUnitsRequired());
        row.put("urgency", safeUpper(request.getUrgency()));
        row.put("status", safeUpper(request.getStatus()));
        row.put("waitMinutes", waitMinutes);
        row.put("waitLabel", waitMinutes >= 60 ? (waitMinutes / 60) + "h in queue" : waitMinutes + "m in queue");
        return row;
    }
}
