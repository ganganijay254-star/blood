package com.bloodcare.bloodcare.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.service.DonorPriorityScoreService;
import com.bloodcare.bloodcare.service.GamificationBadgeService;
import com.bloodcare.bloodcare.service.SmartEligibilityCheckerService;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/features")
public class FeaturesController {

    @Autowired
    private DonorPriorityScoreService priorityScoreService;

    @Autowired
    private SmartEligibilityCheckerService eligibilityService;

    @Autowired
    private GamificationBadgeService badgeService;

    @Autowired
    private DonorRepository donorRepository;

    // ==========================================
    // AI DONOR PRIORITY SCORE
    // ==========================================

    /**
     * Get priority score for a specific donor
     * GET /api/features/priority/{donorId}
     */
    @GetMapping("/priority/{donorId}")
    public ResponseEntity<?> getDonorPriority(@PathVariable Long donorId) {
        Map<String, Object> priorityInfo = priorityScoreService.getDonorPriorityInfo(donorId);
        if (priorityInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(priorityInfo);
    }

    /**
     * Get my priority score (current logged-in user)
     * GET /api/features/my-priority
     */
    @GetMapping("/my-priority")
    public ResponseEntity<?> getMyPriority(HttpSession session) {
        com.bloodcare.bloodcare.entity.User user = 
            (com.bloodcare.bloodcare.entity.User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body("Please login");
        }

        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return ResponseEntity.badRequest().body("No donor profile found");
        }

        Map<String, Object> priorityInfo = priorityScoreService.getDonorPriorityInfo(donor.getId());
        return ResponseEntity.ok(priorityInfo);
    }

    /**
     * Get top priority donors for a blood type
     * GET /api/features/priority-donors?bloodType=O+&limit=10
     */
    @GetMapping("/priority-donors")
    public ResponseEntity<?> getTopPriorityDonors(
            @RequestParam String bloodType,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> topDonors = 
            priorityScoreService.getTopPriorityDonors(bloodType, limit);
        return ResponseEntity.ok(topDonors);
    }

    // ==========================================
    // SMART ELIGIBILITY CHECKER
    // ==========================================

    /**
     * Check eligibility for a specific donor
     * GET /api/features/eligibility/{donorId}
     */
    @GetMapping("/eligibility/{donorId}")
    public ResponseEntity<?> checkDonorEligibility(@PathVariable Long donorId) {
        Map<String, Object> eligibilityReport = 
            eligibilityService.getDetailedEligibilityReport(donorId);
        if (eligibilityReport == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(eligibilityReport);
    }

    /**
     * Check my eligibility (current logged-in user)
     * GET /api/features/my-eligibility
     */
    @GetMapping("/my-eligibility")
    public ResponseEntity<?> getMyEligibility(HttpSession session) {
        com.bloodcare.bloodcare.entity.User user = 
            (com.bloodcare.bloodcare.entity.User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body("Please login");
        }

        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return ResponseEntity.badRequest().body("No donor profile found");
        }

        Map<String, Object> eligibilityReport = eligibilityService.checkEligibility(donor);
        return ResponseEntity.ok(eligibilityReport);
    }

    /**
     * Get bulk eligibility statistics (admin only)
     * GET /api/features/eligibility-stats
     */
    @GetMapping("/eligibility-stats")
    public ResponseEntity<?> getEligibilityStats(HttpSession session) {
        if (session.getAttribute("admin") == null) {
            return ResponseEntity.status(401).body("Admin access required");
        }

        Map<String, Object> stats = eligibilityService.getBulkEligibilityStats();
        return ResponseEntity.ok(stats);
    }

    // ==========================================
    // GAMIFICATION & BADGES
    // ==========================================

    /**
     * Get badges for a specific donor
     * GET /api/features/badges/{donorId}
     */
    @GetMapping("/badges/{donorId}")
    public ResponseEntity<?> getDonorBadges(@PathVariable Long donorId) {
        Donor donor = donorRepository.findById(donorId).orElse(null);
        if (donor == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("badges", badgeService.getDonorBadges(donor));
        result.put("nextBadge", badgeService.getNextBadge(donor));
        return ResponseEntity.ok(result);
    }

    /**
     * Get my badges (current logged-in user)
     * GET /api/features/my-badges
     */
    @GetMapping("/my-badges")
    public ResponseEntity<?> getMyBadges(HttpSession session) {
        com.bloodcare.bloodcare.entity.User user = 
            (com.bloodcare.bloodcare.entity.User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body("Please login");
        }

        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return ResponseEntity.badRequest().body("No donor profile found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("badges", badgeService.getDonorBadges(donor));
        result.put("nextBadge", badgeService.getNextBadge(donor));
        result.put("badgeCount", badgeService.getDonorBadges(donor).size());
        return ResponseEntity.ok(result);
    }

    /**
     * Get leaderboard with badge counts
     * GET /api/features/badge-leaderboard?limit=20
     */
    @GetMapping("/badge-leaderboard")
    public ResponseEntity<?> getBadgeLeaderboard(
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> leaderboard = badgeService.getLeaderboardWithBadges(limit);
        return ResponseEntity.ok(leaderboard);
    }

    // ==========================================
    // DASHBOARD SUMMARY
    // ==========================================

    /**
     * Get complete profile summary (all 3 features)
     * GET /api/features/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getCompleteSummary(HttpSession session) {
        com.bloodcare.bloodcare.entity.User user = 
            (com.bloodcare.bloodcare.entity.User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body("Please login");
        }

        Donor donor = donorRepository.findByUser(user);
        if (donor == null) {
            return ResponseEntity.badRequest().body("No donor profile found");
        }

        Map<String, Object> summary = new HashMap<>();

        // Priority Score
        summary.put("priorityScore", priorityScoreService.getDonorPriorityInfo(donor.getId()));

        // Eligibility
        summary.put("eligibility", eligibilityService.checkEligibility(donor));

        // Badges
        Map<String, Object> badges = new HashMap<>();
        badges.put("earned", badgeService.getDonorBadges(donor));
        badges.put("next", badgeService.getNextBadge(donor));
        summary.put("badges", badges);

        return ResponseEntity.ok(summary);
    }
}
