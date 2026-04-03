package com.bloodcare.bloodcare.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.entity.VisitRequest;
import com.bloodcare.bloodcare.entity.Notification;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamificationBadgeService {

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;
    
    @Autowired
    private com.bloodcare.bloodcare.repository.NotificationRepository notificationRepository;

    @Autowired
    private EmailService emailService;

    // Simple in-memory cache (donorId -> cached badges + timestamp)
    private final Map<Long, CacheEntry> badgeCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Badge definitions with icons
     */
    private enum Badge {
        FIRST_TIME_DONOR("🌟", "First Time Hero", "Made your first donation"),
        BLOOD_WARRIOR("🔴", "Blood Warrior", "Completed 3 donations"),
        GOLD_HERO("👑", "Gold Hero", "Donated 10+ times"),
        PLATINUM_LEGEND("💎", "Platinum Legend", "Donated 20+ times"),
        FAST_RESPONDER("⚡", "Fast Responder", "Responded to urgent blood need within 24 hours"),
        RARE_TYPE_DONOR("💉", "Rare Type Champion", "Donated rare blood type (AB or O-)"),
        CONSISTENT_DONOR("📅", "Consistent Donor", "Perfect attendance - never missed a scheduled donation"),
        GENEROUS_SOUL("❤️", "Generous Soul", "Donated 50+ units of blood"),
        COMMUNITY_CHAMPION("🏅", "Community Champion", "Referred 3+ donors"),
        MONTHLY_MVP("🏆", "Monthly MVP", "Top donor of the month"),
        LIFESAVER("🆘", "Lifesaver", "Donation directly saved a life (emergency need)"),
        STREAKER("🔥", "Streaker", "5 consecutive donations without gap"),
        NEVER_SAY_NO("💪", "Never Say No", "Accepted every donation request in a month"),
        BADGES_COLLECTOR("🎖️", "Badges Collector", "Earned 10+ different badges");

        public final String icon;
        public final String name;
        public final String description;

        Badge(String icon, String name, String description) {
            this.icon = icon;
            this.name = name;
            this.description = description;
        }
    }

    /**
     * Get all badges earned by a donor
     */
    public List<Map<String, Object>> getDonorBadges(Donor donor) {
        if (donor == null) return List.of();

        // Check cache
        CacheEntry entry = badgeCache.get(donor.getId());
        long now = System.currentTimeMillis();
        if (entry != null && (now - entry.timestamp) < CACHE_TTL_MS) {
            return entry.badges;
        }

        List<Map<String, Object>> badges = computeBadges(donor);
        badgeCache.put(donor.getId(), new CacheEntry(badges, now));
        return badges;
    }

    // Compute badges without cache
    private List<Map<String, Object>> computeBadges(Donor donor) {
        List<Map<String, Object>> badges = new ArrayList<>();
        User user = donor.getUser();
        List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED");

        if (approvedVisits.size() >= 1) badges.add(createBadgeMap(Badge.FIRST_TIME_DONOR));
        if (approvedVisits.size() >= 3) badges.add(createBadgeMap(Badge.BLOOD_WARRIOR));
        if (approvedVisits.size() >= 10) badges.add(createBadgeMap(Badge.GOLD_HERO));
        if (approvedVisits.size() >= 20) badges.add(createBadgeMap(Badge.PLATINUM_LEGEND));

        if (hasFastResponded(user)) badges.add(createBadgeMap(Badge.FAST_RESPONDER));

        if (donor.getBloodGroup() != null && (donor.getBloodGroup().equals("AB+") || donor.getBloodGroup().equals("AB-") || donor.getBloodGroup().equals("O-"))) {
            if (approvedVisits.size() >= 1) badges.add(createBadgeMap(Badge.RARE_TYPE_DONOR));
        }

        if (hasNeverMissed(user)) badges.add(createBadgeMap(Badge.CONSISTENT_DONOR));
        if (donor.getUnits() >= 50) badges.add(createBadgeMap(Badge.GENEROUS_SOUL));
        if (hasConsecutiveStreak(user, 5)) badges.add(createBadgeMap(Badge.STREAKER));
        if (hasNeverSaidNo(user)) badges.add(createBadgeMap(Badge.NEVER_SAY_NO));

        if (badges.size() >= 10) badges.add(createBadgeMap(Badge.BADGES_COLLECTOR));

        return badges;
    }

    // Entry used by cache
    private static class CacheEntry {
        final List<Map<String, Object>> badges;
        final long timestamp;
        CacheEntry(List<Map<String, Object>> badges, long timestamp) { this.badges = badges; this.timestamp = timestamp; }
    }

    /**
     * Run badge calculation for all donors and notify newly earned badges.
     * This can be scheduled or triggered by admin.
     */
    public Map<String, Object> awardBadgesToAllDonors() {
        List<Donor> all = donorRepository.findAll();
        int notifiedCount = 0;
        for (Donor d : all) {
            List<Map<String, Object>> current = computeBadges(d);
            CacheEntry prev = badgeCache.get(d.getId());
            Set<String> prevNames = prev == null ? Collections.emptySet() : prev.badges.stream().map(b -> (String) b.get("name")).collect(Collectors.toSet());

            for (Map<String, Object> b : current) {
                String name = (String) b.get("name");
                if (!prevNames.contains(name)) {
                    // New badge earned -> create notification + email
                    if (d.getUser() != null) {
                        Notification n = new Notification(d.getUser(), "New Badge: " + name, "Congratulations! You earned the '" + name + "' badge.", "BADGE");
                        notificationRepository.save(n);
                        try {
                            emailService.sendEligibleDonorEmail(d.getUser().getEmail(), d.getUser().getName(), d.getBloodGroup(), d.getCity());
                        } catch (Exception ignored) {}
                        notifiedCount++;
                    }
                }
            }

            // Update cache
            badgeCache.put(d.getId(), new CacheEntry(current, System.currentTimeMillis()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalDonors", all.size());
        result.put("notifiedCount", notifiedCount);
        return result;
    }

    /**
     * Get next achievable badge for a donor
     */
    public Map<String, Object> getNextBadge(Donor donor) {
        List<Map<String, Object>> earnedBadges = getDonorBadges(donor);
        Set<String> earnedNames = earnedBadges.stream()
                .map(b -> (String) b.get("name"))
                .collect(Collectors.toSet());

        User user = donor.getUser();
        List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED");

        // Suggest next badge based on progress
        if (approvedVisits.size() < 3 && !earnedNames.contains(Badge.BLOOD_WARRIOR.name)) {
            return createBadgeWithProgress(Badge.BLOOD_WARRIOR, approvedVisits.size(), 3);
        }

        if (approvedVisits.size() < 10 && !earnedNames.contains(Badge.GOLD_HERO.name)) {
            return createBadgeWithProgress(Badge.GOLD_HERO, approvedVisits.size(), 10);
        }

        if (donor.getUnits() < 50 && !earnedNames.contains(Badge.GENEROUS_SOUL.name)) {
            return createBadgeWithProgress(Badge.GENEROUS_SOUL, donor.getUnits(), 50);
        }

        return null; // All major badges earned
    }

    /**
     * Get leaderboard with badge counts
     */
    public List<Map<String, Object>> getLeaderboardWithBadges(int limit) {
        List<Donor> allDonors = donorRepository.findAll();

        return allDonors.stream()
                .map(donor -> {
                    List<Map<String, Object>> badges = getDonorBadges(donor);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("donorId", donor.getId());
                    entry.put("name", donor.getUser().getName());
                    entry.put("badgeCount", badges.size());
                    entry.put("badges", badges);
                    entry.put("donations", visitRequestRepository
                            .findByUserAndStatus(donor.getUser(), "APPROVED").size());
                    entry.put("units", donor.getUnits());
                    return entry;
                })
                .sorted((a, b) -> Integer.compare((int) b.get("badgeCount"), (int) a.get("badgeCount")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ========== Helper Methods ==========

    private boolean hasFastResponded(User user) {
        List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED");
        for (VisitRequest visit : approvedVisits) {
            if (visit.getRequestDate() != null) {
                long hoursResponded = ChronoUnit.HOURS.between(visit.getRequestDate().atStartOfDay(), 
                                                               LocalDate.now().atStartOfDay());
                if (hoursResponded <= 24) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasNeverMissed(User user) {
        List<VisitRequest> allVisits = visitRequestRepository.findByUser(user);
        if (allVisits.size() < 3) {
            return false; // Need at least 3 to prove consistency
        }

        // Count rejections - if none, they never missed
        long rejections = allVisits.stream()
                .filter(v -> "REJECTED".equals(v.getStatus()))
                .count();

        return rejections == 0;
    }

    private boolean hasConsecutiveStreak(User user, int streakLength) {
        List<VisitRequest> approvedVisits = visitRequestRepository.findByUserAndStatus(user, "APPROVED")
                .stream()
                .sorted(Comparator.comparing(VisitRequest::getRequestDate).reversed())
                .toList();

        if (approvedVisits.size() < streakLength) {
            return false;
        }

        // Check last 'streakLength' donations for no gaps > 120 days
        int consecutiveCount = 0;
        for (int i = 0; i < approvedVisits.size() - 1; i++) {
            LocalDate current = approvedVisits.get(i).getRequestDate();
            LocalDate previous = approvedVisits.get(i + 1).getRequestDate();

            if (current != null && previous != null) {
                long daysBetween = ChronoUnit.DAYS.between(previous, current);
                if (daysBetween <= 150) { // Allow some buffer
                    consecutiveCount++;
                    if (consecutiveCount >= streakLength - 1) {
                        return true;
                    }
                } else {
                    consecutiveCount = 0;
                }
            }
        }

        return false;
    }

    private boolean hasNeverSaidNo(User user) {
        List<VisitRequest> lastMonthRequests = visitRequestRepository.findByUser(user)
                .stream()
                .filter(v -> v.getRequestDate() != null)
                .filter(v -> ChronoUnit.DAYS.between(v.getRequestDate(), LocalDate.now()) <= 30)
                .toList();

        if (lastMonthRequests.isEmpty()) {
            return false;
        }

        // All requests must be approved
        long approvedCount = lastMonthRequests.stream()
                .filter(v -> "APPROVED".equals(v.getStatus()))
                .count();

        return approvedCount == lastMonthRequests.size();
    }

    private Map<String, Object> createBadgeMap(Badge badge) {
        Map<String, Object> badgeMap = new HashMap<>();
        badgeMap.put("icon", badge.icon);
        badgeMap.put("name", badge.name);
        badgeMap.put("description", badge.description);
        return badgeMap;
    }

    private Map<String, Object> createBadgeWithProgress(Badge badge, int current, int target) {
        Map<String, Object> badgeMap = new HashMap<>();
        badgeMap.put("icon", badge.icon);
        badgeMap.put("name", badge.name);
        badgeMap.put("description", badge.description);
        badgeMap.put("progress", current);
        badgeMap.put("target", target);
        badgeMap.put("percentage", (current * 100) / target);
        return badgeMap;
    }
}
