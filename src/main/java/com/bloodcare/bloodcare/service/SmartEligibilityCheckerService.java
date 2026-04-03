package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.Notification;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.NotificationRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SmartEligibilityCheckerService {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    /**
     * Find donors matching blood group + city and notify eligible donors.
     * Returns a list of matched donors (basic info and eligibility).
     */
    public List<Map<String, Object>> matchAndNotify(String bloodGroup, String city, int unitsNeeded, int limit) {
        List<Donor> all = donorRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        String requestCity = normalize(city);
        String requestBloodGroup = normalize(bloodGroup);

        for (Donor d : all) {
            if (d == null) continue;
            String donorBloodGroup = normalize(d.getBloodGroup());
            if (donorBloodGroup.isEmpty() || !donorBloodGroup.equals(requestBloodGroup)) continue;

            // City matching should be resilient to spacing/case differences.
            if (!requestCity.isEmpty()) {
                String donorCity = normalize(d.getCity());
                if (!donorCity.isEmpty() && !donorCity.equals(requestCity)) continue;
            }
            // Do not hard-block donors by units because multiple donors can fulfill one request.

            boolean eligible = isEligibleToDonate(d);

            Map<String, Object> info = new HashMap<>();
            info.put("id", d.getId());
            info.put("name", d.getUser() != null ? d.getUser().getName() : "");
            info.put("email", d.getUser() != null ? d.getUser().getEmail() : "");
            info.put("bloodGroup", d.getBloodGroup());
            info.put("city", d.getCity());
            info.put("units", d.getUnits());
            info.put("isEligible", eligible);

            // Notify every matched donor immediately (no admin approval dependency)
            if (d.getUser() != null && d.getUser().getEmail() != null) {
                try {
                    emailService.sendEligibleDonorEmail(d.getUser().getEmail(), d.getUser().getName(), d.getBloodGroup(), d.getCity());

                    Notification n = new Notification(d.getUser(),
                            "Blood Request Match",
                            "A nearby blood request matches your blood group and city. Please respond from your donor dashboard.",
                            "GENERAL",
                            d.getBloodGroup(),
                            d.getUnits(),
                            null,
                            d.getCity(),
                            null);
                    notificationRepository.save(n);
                    info.put("notified", true);
                } catch (Exception ex) {
                    info.put("notified", false);
                    info.put("notifyError", ex.getMessage());
                }
            } else {
                info.put("notified", false);
            }

            results.add(info);
            if (results.size() >= limit) break;
        }

        return results;
    }

    public List<Map<String, Object>> sendEmergencyNotificationToAll(String bloodGroup, String city, String requestLabel) {
        List<Map<String, Object>> results = new ArrayList<>();
        String requestCity = normalize(city);

        for (Donor donor : donorRepository.findByAvailableTrue()) {
            if (donor == null || donor.getUser() == null) continue;

            String donorCity = normalize(donor.getCity());
            boolean nearby = requestCity.isEmpty() || donorCity.isEmpty() || donorCity.equals(requestCity);
            if (!nearby) {
                continue;
            }

            Map<String, Object> info = new HashMap<>();
            info.put("id", donor.getId());
            info.put("name", donor.getUser().getName());
            info.put("email", donor.getUser().getEmail());
            info.put("bloodGroup", donor.getBloodGroup());
            info.put("city", donor.getCity());

            try {
                if (donor.getUser().getEmail() != null && !donor.getUser().getEmail().isBlank()) {
                    emailService.sendEligibleDonorEmail(
                            donor.getUser().getEmail(),
                            donor.getUser().getName(),
                            bloodGroup,
                            city);
                }

                Notification notification = new Notification(
                        donor.getUser(),
                        "Emergency Blood Alert",
                        "Urgent request " + (requestLabel == null ? "" : requestLabel + " ")
                                + "needs support. Please check your donor dashboard immediately.",
                        "ALERT",
                        bloodGroup,
                        donor.getUnits(),
                        null,
                        donor.getCity(),
                        null);
                notificationRepository.save(notification);
                info.put("notified", true);
            } catch (Exception ex) {
                info.put("notified", false);
                info.put("notifyError", ex.getMessage());
            }

            results.add(info);
        }

        return results;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // Duplicate eligibility logic (matches DonorPriorityScoreService rules)
    private boolean isEligibleToDonate(Donor donor) {
        if (donor == null) return false;
        LocalDate last = donor.getLastDonationDate();
        if (last == null) return true;
        int gapRequired = "FEMALE".equalsIgnoreCase(donor.getGender()) ? 120 : 90;
        long days = ChronoUnit.DAYS.between(last, LocalDate.now());
        return days >= gapRequired;
    }

    /**
     * Detailed eligibility check returning structured report
     */
    public Map<String, Object> checkEligibility(Donor donor) {
        Map<String, Object> result = new HashMap<>();
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (donor == null) {
            result.put("isEligible", false);
            result.put("reason", "Donor not found");
            result.put("eligibilityScore", 0);
            return result;
        }

        // Medical gap
        LocalDate last = donor.getLastDonationDate();
        if (last != null) {
            int gapRequired = "FEMALE".equalsIgnoreCase(donor.getGender()) ? 120 : 90;
            long days = ChronoUnit.DAYS.between(last, LocalDate.now());
            if (days < gapRequired) {
                issues.add("Medical gap not met: " + days + " / " + gapRequired + " days");
            }
        }

        // Pending requests
        if (donor.getUser() != null) {
            long pending = visitRequestRepository.findByUserAndStatus(donor.getUser(), "PENDING").size();
            if (pending > 0) warnings.add("You have pending donation request(s)");
        }

        int score = issues.isEmpty() ? 100 : 40;

        result.put("isEligible", issues.isEmpty());
        result.put("eligibilityScore", score);
        result.put("blockers", issues);
        result.put("warnings", warnings);
        result.put("nextEligibleDate", calculateNextEligibleDate(donor));
        result.put("daysUntilEligible", calculateDaysUntilEligible(donor));

        return result;
    }

    public Map<String, Object> getDetailedEligibilityReport(Long donorId) {
        Donor donor = donorRepository.findById(donorId).orElse(null);
        if (donor == null) return null;
        return checkEligibility(donor);
    }

    public Map<String, Object> getBulkEligibilityStats() {
        List<Donor> all = donorRepository.findAll();
        int total = all.size();
        int eligible = 0;
        for (Donor d : all) {
            if ((boolean) checkEligibility(d).get("isEligible")) eligible++;
        }
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDonors", total);
        stats.put("eligibleCount", eligible);
        stats.put("ineligibleCount", total - eligible);
        stats.put("eligibilityPercentage", total > 0 ? (eligible * 100 / total) : 0);
        return stats;
    }

    private LocalDate calculateNextEligibleDate(Donor donor) {
        LocalDate last = donor.getLastDonationDate();
        if (last == null) return LocalDate.now();
        int gapRequired = "FEMALE".equalsIgnoreCase(donor.getGender()) ? 120 : 90;
        return last.plusDays(gapRequired);
    }

    private int calculateDaysUntilEligible(Donor donor) {
        LocalDate next = calculateNextEligibleDate(donor);
        long days = ChronoUnit.DAYS.between(LocalDate.now(), next);
        return Math.max(0, (int) days);
    }
}
