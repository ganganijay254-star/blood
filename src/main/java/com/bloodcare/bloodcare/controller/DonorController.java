package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.dto.DonorFormRequest;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;
import com.bloodcare.bloodcare.service.DonorSearchService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donor")
public class DonorController {

    private static final Set<String> ALLOWED_BLOOD_GROUPS = Set.of(
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final Set<String> ALLOWED_GENDERS = Set.of("MALE", "FEMALE");
    private static final Set<String> ALLOWED_SMOKING_STATUS = Set.of("YES", "NO");

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    @Autowired
    private DonorSearchService donorSearchService;

    // =====================
    // SAVE DONOR FORM
    // =====================
    @PostMapping("/save")
    public ResponseEntity<?> saveDonor(
            @RequestBody DonorFormRequest request,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(errorResponse("Please login"));
        }

        if (!AdminController.isControlEnabled("enableDonations", true)
                || AdminController.isControlEnabled("maintenanceMode", false)) {
            return ResponseEntity.badRequest().body(errorResponse("Donor registration is currently disabled by admin"));
        }

        Donor existing = donorRepository.findByUser(user);
        if (existing != null) {
            Map<String, Object> response = errorResponse("Your donor profile already exists.");
            response.put("code", "DONOR_ALREADY_EXISTS");
            response.put("redirectUrl", "/donor-dashboard");
            response.put("profileUrl", "/donor-profile");
            return ResponseEntity.status(409).body(response);
        }

        int minAge = AdminController.getSettingInt("minAge", 18);
        int maxAge = AdminController.getSettingInt("maxAge", 65);
        int minWeight = AdminController.getSettingInt("minWeight", 50);

        Map<String, String> fieldErrors = validateDonorRequest(request, minAge, maxAge, minWeight);
        if (!fieldErrors.isEmpty()) {
            return ResponseEntity.badRequest().body(validationErrorResponse(fieldErrors));
        }

        Donor donor = new Donor();
        donor.setUser(user);
        donor.setBloodGroup(normalizeUpper(request.getBloodGroup()));
        donor.setGender(normalizeUpper(request.getGender()));
        donor.setAge(request.getAge());
        donor.setWeight(request.getWeight());
        donor.setCity(request.getCity().trim());
        donor.setSmoking(normalizeUpper(request.getSmoking()));
        donor.setUnits(1);
        donor.setLastDonationDate(request.getLastDonationDate());

        Donor saved = donorRepository.save(donor);
        return ResponseEntity.ok(saved);
    }

    // =====================
    // GET MY DONOR PROFILE
    // =====================
    @GetMapping("/me")
    public ResponseEntity<?> myDonor(HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body("Please login");
        }

        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(donor);
    }

    // =====================
    // PUBLIC LEADERBOARD
    // =====================
    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard() {
        if (!AdminController.isControlEnabled("showLeaderboard", true)) {
            return ResponseEntity.status(403).body("Leaderboard is disabled");
        }

        int pointsPerUnit = AdminController.getSettingInt("pointsPerUnit", 100);
        int agePointsMultiplier = AdminController.getSettingInt("agePointsMultiplier", 5);
        int bonusPerDonation = AdminController.getSettingInt("bonusPerDonation", 200);
        int minAge = AdminController.getSettingInt("minAge", 18);
        int maxAge = AdminController.getSettingInt("maxAge", 65);
        int leaderboardSize = AdminController.getSettingInt("leaderboardSize", 10);

        List<Map<String, Object>> data = donorRepository.findAll()
                .stream()
                .filter(d -> d.getUser() != null)
                .map(d -> {
                    User user = d.getUser();
                    int age = d.getAge();

                    int totalUnitsDonated = (int) visitRequestRepository.findByUser(user).stream()
                            .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus())
                                    && "DONOR".equalsIgnoreCase(v.getRequestType()))
                            .mapToInt(VisitRequest::getUnits)
                            .sum();

                    long donationCount = visitRequestRepository.findByUser(user).stream()
                            .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus())
                                    && "DONOR".equalsIgnoreCase(v.getRequestType()))
                            .count();

                    int donationPoints = Math.max(totalUnitsDonated, 0) * pointsPerUnit;

                    int agePoints = 0;
                    if (age >= minAge && age <= maxAge) {
                        agePoints = (age - minAge) * agePointsMultiplier;
                    }

                    int donationBonus = (int) Math.max(donationCount, 0) * bonusPerDonation;
                    int totalPoints = donationPoints + agePoints + donationBonus;

                    String tier = resolveTier(totalUnitsDonated);
                    String reward = resolveReward(tier);

                    Map<String, Object> row = new HashMap<>();
                    row.put("id", d.getId());
                    row.put("name", user.getName() == null ? "Donor" : user.getName());
                    row.put("bloodGroup", d.getBloodGroup() == null ? "--" : d.getBloodGroup());
                    row.put("gender", d.getGender() == null ? "--" : d.getGender());
                    row.put("age", age);
                    row.put("city", d.getCity() == null ? "--" : d.getCity());
                    row.put("totalUnitsDonated", totalUnitsDonated);
                    row.put("donationCount", donationCount);
                    row.put("points", totalPoints);
                    row.put("donationPoints", donationPoints);
                    row.put("agePoints", agePoints);
                    row.put("donationBonus", donationBonus);
                    row.put("tier", tier);
                    row.put("reward", reward);
                    row.put("lastDonationDate", d.getLastDonationDate() == null ? "-" : d.getLastDonationDate().toString());
                    return row;
                })
                .sorted((Map<String, Object> a, Map<String, Object> b) ->
                        Integer.compare(((Number) b.get("points")).intValue(),
                                ((Number) a.get("points")).intValue()))
                .limit(Math.max(1, leaderboardSize))
                .collect(Collectors.toList());

        return ResponseEntity.ok(data);
    }

    // =====================
    // SEARCH DONORS (BLOOD GROUP + CITY + AVAILABILITY)
    // =====================
    @GetMapping("/search")
    public ResponseEntity<?> searchDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "true") boolean available) {

        List<Donor> donors = donorSearchService.search(bloodGroup, city, available);
        return ResponseEntity.ok(donors);
    }

    // =====================
    // NEAREST DONOR SEARCH
    // =====================
    @GetMapping("/nearby")
    public ResponseEntity<?> nearbyDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String city,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> donors = donorSearchService.findNearestDonors(bloodGroup, city, lat, lng, limit);
        return ResponseEntity.ok(donors);
    }

    // =====================
    // DONATION HISTORY TRACKING
    // =====================
    @GetMapping("/history")
    public ResponseEntity<?> donationHistory(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body("Please login");
        }

        List<Map<String, Object>> history = visitRequestRepository.findByUser(user).stream()
                .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus()))
                .filter(v -> "DONOR".equalsIgnoreCase(v.getRequestType()))
                .map(v -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("visitId", v.getId());
                    row.put("hospital", v.getHospitalName());
                    row.put("units", v.getUnits());
                    row.put("status", v.getStatus());
                    row.put("requestDate", v.getRequestDate());
                    LocalDate date = v.getRequestDate();
                    row.put("donationDate", date);
                    return row;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    private Map<String, String> validateDonorRequest(
            DonorFormRequest request,
            int minAge,
            int maxAge,
            int minWeight) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        if (request == null) {
            fieldErrors.put("form", "Invalid donor form payload.");
            return fieldErrors;
        }

        String bloodGroup = normalizeUpper(request.getBloodGroup());
        if (!ALLOWED_BLOOD_GROUPS.contains(bloodGroup)) {
            fieldErrors.put("bloodGroup", "Select a valid blood group.");
        }

        String gender = normalizeUpper(request.getGender());
        if (!ALLOWED_GENDERS.contains(gender)) {
            fieldErrors.put("gender", "Select a valid gender.");
        }

        Integer age = request.getAge();
        if (age == null) {
            fieldErrors.put("age", "Enter your age.");
        } else if (age < minAge || age > maxAge) {
            fieldErrors.put("age", "Age must be between " + minAge + " and " + maxAge + ".");
        }

        Double weight = request.getWeight();
        if (weight == null) {
            fieldErrors.put("weight", "Enter your weight.");
        } else if (weight < minWeight) {
            fieldErrors.put("weight", "Minimum weight must be " + minWeight + " kg.");
        }

        String city = request.getCity() == null ? "" : request.getCity().trim();
        if (city.isEmpty()) {
            fieldErrors.put("city", "Select your city.");
        }

        String smoking = normalizeUpper(request.getSmoking());
        if (!ALLOWED_SMOKING_STATUS.contains(smoking)) {
            fieldErrors.put("smoking", "Select your smoking status.");
        } else if ("YES".equals(smoking)) {
            fieldErrors.put("smoking", "Smokers are not eligible to register as donors.");
        }

        LocalDate lastDonationDate = request.getLastDonationDate();
        LocalDate today = LocalDate.now();
        if (lastDonationDate != null) {
            if (lastDonationDate.isAfter(today)) {
                fieldErrors.put("lastDonation", "Last donation date cannot be in the future.");
            } else if ("MALE".equals(gender) && ChronoUnit.DAYS.between(lastDonationDate, today) < 90) {
                fieldErrors.put("lastDonation", "Male donors need a minimum 90 day gap.");
            } else if ("FEMALE".equals(gender) && ChronoUnit.DAYS.between(lastDonationDate, today) < 120) {
                fieldErrors.put("lastDonation", "Female donors need a minimum 120 day gap.");
            }
        }

        return fieldErrors;
    }

    private Map<String, Object> validationErrorResponse(Map<String, String> fieldErrors) {
        Map<String, Object> response = errorResponse("Please correct the highlighted fields.");
        response.put("fieldErrors", fieldErrors);
        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        return response;
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String resolveTier(int totalUnitsDonated) {
        if (totalUnitsDonated >= 10) return "gold";
        if (totalUnitsDonated >= 5) return "silver";
        if (totalUnitsDonated >= 1) return "bronze";
        return "rookie";
    }

    private String resolveReward(String tier) {
        java.util.List<String> rewardOptions;

        if ("gold".equals(tier)) {
            rewardOptions = java.util.Arrays.asList(
                "Free Full Body Checkup",
                "Free Blood Test",
                "50% Medicine Discount",
                "VIP Donor Badge"
            );
        } else if ("silver".equals(tier)) {
            rewardOptions = java.util.Arrays.asList(
                "30% Medicine Discount",
                "Priority Hospital Access",
                "Health Supplement Pack",
                "Gift Voucher Rs 500"
            );
        } else if ("bronze".equals(tier)) {
            rewardOptions = java.util.Arrays.asList(
                "Cash Reward Rs 200",
                "Food Voucher Rs 300",
                "100 Loyalty Points",
                "1 Month Health App Premium"
            );
        } else {
            rewardOptions = java.util.Arrays.asList(
                "Participation Badge",
                "Donor Certificate",
                "Music Streaming Trial",
                "E-Book Collection Access"
            );
        }

        int randomIndex = new java.util.Random().nextInt(rewardOptions.size());
        return rewardOptions.get(randomIndex);
    }
}
