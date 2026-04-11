package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.Reward;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.RewardRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    /* ===== REWARD DEFINITIONS ===== */
    private static final List<Map<String, Object>> REWARD_DEFINITIONS = new ArrayList<>();

    static {
        // Premium Rewards
        REWARD_DEFINITIONS.add(createRewardDef("💉 Free Full Body Checkup", 
            "Complete health checkup at any partnered hospital", "checkup", "💉", 500));
        REWARD_DEFINITIONS.add(createRewardDef("🏥 Free Blood Test", 
            "Comprehensive blood test worth ₹1500", "health", "🏥", 300));
        REWARD_DEFINITIONS.add(createRewardDef("💊 Medicine Discount (50%)", 
            "50% discount on all medicines for 1 month", "discount", "💊", 400));
        REWARD_DEFINITIONS.add(createRewardDef("🍎 Health Supplement Pack", 
            "Free multivitamin and iron supplement pack", "supplement", "🍎", 250));
        
        // Mid-tier Rewards
        REWARD_DEFINITIONS.add(createRewardDef("💳 Medicine Discount (30%)", 
            "30% discount on all medicines for 2 weeks", "discount", "💳", 200));
        REWARD_DEFINITIONS.add(createRewardDef("🏅 Priority Hospital Access", 
            "Priority slot booking at partnered hospitals", "priority", "🏅", 250));
        REWARD_DEFINITIONS.add(createRewardDef("📱 Health App Premium (1 month)", 
            "Free premium access to health tracking app", "digital", "📱", 180));
        REWARD_DEFINITIONS.add(createRewardDef("🩹 First Aid Kit", 
            "Complete first aid kit for home emergency", "kit", "🩹", 150));
        
        // Entry-level Rewards
        REWARD_DEFINITIONS.add(createRewardDef("💰 Cash Reward ₹200", 
            "Direct cash reward", "cash", "💰", 200));
        REWARD_DEFINITIONS.add(createRewardDef("🎁 Gift Voucher ₹500", 
            "E-commerce gift voucher", "voucher", "🎁", 300));
        REWARD_DEFINITIONS.add(createRewardDef("🍔 Food Voucher ₹300", 
            "Restaurant food voucher", "food", "🍔", 150));
        REWARD_DEFINITIONS.add(createRewardDef("⭐ Loyalty Points x100", 
            "100 loyalty points for future purchases", "points", "⭐", 100));
        
        // Special Rewards
        REWARD_DEFINITIONS.add(createRewardDef("🏆 VIP Donor Badge", 
            "Exclusive badge and recognition", "badge", "🏆", 350));
        REWARD_DEFINITIONS.add(createRewardDef("📸 Donor Certificate", 
            "Special framed certificate of appreciation", "certificate", "📸", 150));
        REWARD_DEFINITIONS.add(createRewardDef("🎵 Music Streaming (3 months)", 
            "Free premium music streaming subscription", "entertainment", "🎵", 280));
        REWARD_DEFINITIONS.add(createRewardDef("📚 E-Book Collection", 
            "Access to 100+ health and wellness e-books", "education", "📚", 200));
    }

    private static Map<String, Object> createRewardDef(String title, String description, 
                                                        String category, String icon, int value) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("category", category);
        map.put("icon", icon);
        map.put("value", value);
        return map;
    }

    /* ===== GET MY REWARDS ===== */
    @GetMapping("/my-rewards")
    public ResponseEntity<?> getMyRewards(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            List<Reward> rewards = rewardRepository.findByUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("active", rewards.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(Collectors.toList()));
            response.put("redeemed", rewards.stream()
                .filter(r -> "REDEEMED".equals(r.getStatus()))
                .collect(Collectors.toList()));
            response.put("total", rewards.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching rewards: " + e.getMessage());
        }
    }

    /* ===== GENERATE NEW RANDOM REWARD ===== */
    @PostMapping("/generate")
    public ResponseEntity<?> generateReward(
            @RequestParam(required = false) Long visitId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            // Count APPROVED visits (actual donations completed)
            List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED");
            int totalApprovedDonations = approvedVisits.size();
            
            if (totalApprovedDonations < 1) {
                return ResponseEntity.badRequest()
                    .body("You need to complete at least 1 donation to earn rewards");
            }

            // Count active and redeemed rewards
            List<Reward> allRewards = rewardRepository.findByUser(user);
            int totalRewardsEarned = allRewards.size();
            
            // User should only have as many rewards as donations completed
            if (totalRewardsEarned >= totalApprovedDonations) {
                return ResponseEntity.badRequest()
                    .body("You have already earned " + totalRewardsEarned + " reward(s) for " + totalApprovedDonations + " donation(s). Complete more donations to earn more rewards!");
            }

            // Check if user already has an ACTIVE reward - prevent multiple active at once
            List<Reward> activeRewards = rewardRepository.findByUserAndStatus(user, "ACTIVE");
            if (!activeRewards.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("You already have an active reward. Redeem it first before getting a new one!");
            }

            // Get random reward from definitions
            Random random = new Random();
            Map<String, Object> rewardDef = REWARD_DEFINITIONS.get(
                random.nextInt(REWARD_DEFINITIONS.size())
            );

            // Create new reward
            String rewardCode = generateRewardCode();
            Reward reward = new Reward(
                user,
                (String) rewardDef.get("title"),
                (String) rewardDef.get("description"),
                rewardCode,
                (String) rewardDef.get("category"),
                (String) rewardDef.get("icon"),
                (Integer) rewardDef.get("value")
            );

            reward = rewardRepository.save(reward);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reward", reward);
            response.put("message", "🎉 Congratulations! You earned reward " + (totalRewardsEarned + 1) + " of " + totalApprovedDonations + "!");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating reward: " + e.getMessage());
        }
    }

    /* ===== REDEEM REWARD ===== */
    @PostMapping("/redeem/{rewardId}")
    public ResponseEntity<?> redeemReward(
            @PathVariable Long rewardId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            Reward reward = rewardRepository.findById(rewardId)
                .orElse(null);

            if (reward == null) {
                return ResponseEntity.badRequest().body("Reward not found");
            }

            if (!reward.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }

            if ("REDEEMED".equals(reward.getStatus())) {
                return ResponseEntity.badRequest().body("Reward already redeemed");
            }

            // Redeem the reward
            reward.setStatus("REDEEMED");
            reward.setRedeemedDate(LocalDateTime.now());
            reward = rewardRepository.save(reward);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("reward", reward);
            response.put("message", "✅ Reward redeemed successfully! Your code: " + reward.getRewardCode());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error redeeming reward: " + e.getMessage());
        }
    }

    /* ===== GET REWARD DEFINITIONS ===== */
    @GetMapping("/definitions")
    public ResponseEntity<?> getRewardDefinitions() {
        return ResponseEntity.ok(REWARD_DEFINITIONS);
    }

    /* ===== HELPER METHODS ===== */
    private String generateRewardCode() {
        return "RC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
