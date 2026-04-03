package com.bloodcare.bloodcare.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DonorPriorityScoreService {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    /**
     * Calculate overall priority score for a donor (0-100)
     * Factors: Eligibility, Consistency, Units Donated, Recency, Availability
     */
    public int calculatePriorityScore(Donor donor) {
        int score = 0;

        if (donor == null || donor.getUser() == null) {
            return 0;
        }

        // 1. Eligibility Bonus (25 points max)
        score += calculateEligibilityBonus(donor);

        // 2. Donation Consistency (20 points max)
        score += calculateConsistencyScore(donor);

        // 3. Total Units Donated (20 points max)
        score += calculateUnitsScore(donor);

        // 4. Recency Score (20 points max)
        score += calculateRecencyScore(donor);

        // 5. Availability Bonus (15 points max)
        score += calculateAvailabilityBonus(donor);

        return Math.min(100, score);
    }

    // ✅ Eligibility Bonus
    private int calculateEligibilityBonus(Donor donor) {
        if (!isEligibleToDonate(donor)) {
            return 0;
        }

        LocalDate lastDonation = donor.getLastDonationDate();
        if (lastDonation == null) {
            return 25; // Never donated = highest priority
        }

        long daysSinceLastDonation = ChronoUnit.DAYS.between(lastDonation, LocalDate.now());
        int gapRequired = donor.getGender().equals("FEMALE") ? 120 : 90;

        // More overdue = higher priority
        if (daysSinceLastDonation > gapRequired + 180) {
            return 25; // Way overdue
        } else if (daysSinceLastDonation > gapRequired + 90) {
            return 20;
        } else if (daysSinceLastDonation > gapRequired) {
            return 15;
        } else {
            return 5;
        }
    }

    // ✅ Consistency Score (frequent donators)
    private int calculateConsistencyScore(Donor donor) {
        User user = donor.getUser();
        List<VisitRequest> approvedVisits = visitRequestRepository
                .findByUserAndStatus(user, "APPROVED");

        int visitCount = approvedVisits.size();

        if (visitCount >= 10) {
            return 20; // Loyal donor
        } else if (visitCount >= 5) {
            return 15;
        } else if (visitCount >= 2) {
            return 10;
        } else if (visitCount == 1) {
            return 5;
        } else {
            return 0;
        }
    }

    // ✅ Units Score (more donated = higher priority)
    private int calculateUnitsScore(Donor donor) {
        int totalUnits = donor.getUnits();

        if (totalUnits >= 20) {
            return 20;
        } else if (totalUnits >= 15) {
            return 15;
        } else if (totalUnits >= 10) {
            return 10;
        } else if (totalUnits >= 5) {
            return 5;
        } else {
            return 0;
        }
    }

    // ✅ Recency Score (more recent = lower priority, to balance spread)
    private int calculateRecencyScore(Donor donor) {
        LocalDate lastDonation = donor.getLastDonationDate();

        if (lastDonation == null) {
            return 20; // Never donated = highest priority
        }

        long daysSinceLastDonation = ChronoUnit.DAYS.between(lastDonation, LocalDate.now());

        if (daysSinceLastDonation > 365) {
            return 20; // Very long time ago
        } else if (daysSinceLastDonation > 180) {
            return 15;
        } else if (daysSinceLastDonation > 90) {
            return 10;
        } else if (daysSinceLastDonation > 30) {
            return 5;
        } else {
            return 0; // Very recent
        }
    }

    // ✅ Availability Bonus
    private int calculateAvailabilityBonus(Donor donor) {
        // Check if donor is actively participating (pendingvisits, etc)
        User user = donor.getUser();
        List<VisitRequest> pendingVisits = visitRequestRepository
                .findByUserAndStatus(user, "PENDING");

        if (pendingVisits.size() == 0) {
            return 15; // Available = high priority
        } else if (pendingVisits.size() == 1) {
            return 10;
        } else {
            return 5;
        }
    }

    // ✅ Check if donor is eligible
    private boolean isEligibleToDonate(Donor donor) {
        LocalDate lastDonation = donor.getLastDonationDate();

        if (lastDonation == null) {
            return true; // Never donated
        }

        int gapRequired = donor.getGender().equals("FEMALE") ? 120 : 90;
        long daysSinceLastDonation = ChronoUnit.DAYS.between(lastDonation, LocalDate.now());

        return daysSinceLastDonation >= gapRequired;
    }

    /**
     * Get top N priority donors for a blood type
     */
    public List<Map<String, Object>> getTopPriorityDonors(String bloodType, int limit) {
        List<Donor> allDonors = donorRepository.findAll();

        return allDonors.stream()
                .filter(d -> d.getBloodGroup() != null && d.getBloodGroup().equals(bloodType))
                .map(d -> {
                    Map<String, Object> donorInfo = new HashMap<>();
                    donorInfo.put("id", d.getId());
                    donorInfo.put("name", d.getUser().getName());
                    donorInfo.put("email", d.getUser().getEmail());
                    donorInfo.put("bloodGroup", d.getBloodGroup());
                    donorInfo.put("priorityScore", calculatePriorityScore(d));
                    donorInfo.put("isEligible", isEligibleToDonate(d));
                    donorInfo.put("units", d.getUnits());
                    return donorInfo;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("priorityScore"), 
                                                  (int) a.get("priorityScore")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get priority rank for a specific donor
     */
    public Map<String, Object> getDonorPriorityInfo(Long donorId) {
        Donor donor = donorRepository.findById(donorId).orElse(null);
        if (donor == null) {
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("donorId", donor.getId());
        info.put("name", donor.getUser().getName());
        info.put("priorityScore", calculatePriorityScore(donor));
        info.put("isEligible", isEligibleToDonate(donor));
        info.put("bloodGroup", donor.getBloodGroup());
        info.put("totalUnitsDonated", donor.getUnits());
        info.put("lastDonationDate", donor.getLastDonationDate());
        info.put("donationCount", visitRequestRepository.findByUserAndStatus(donor.getUser(), "APPROVED").size());

        return info;
    }
}
