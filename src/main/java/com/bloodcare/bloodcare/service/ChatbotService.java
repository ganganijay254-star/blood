package com.bloodcare.bloodcare.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.Certificate;
import com.bloodcare.bloodcare.entity.ChatbotMessage;
import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.BloodRequestRepository;
import com.bloodcare.bloodcare.repository.CertificateRepository;
import com.bloodcare.bloodcare.repository.ChatbotMessageRepository;
import com.bloodcare.bloodcare.repository.DonorRepository;
import com.bloodcare.bloodcare.repository.UserRepository;
import com.bloodcare.bloodcare.repository.VisitRequestRepository;

@Service
public class ChatbotService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private VisitRequestRepository visitRequestRepository;

    @Autowired
    private ChatbotMessageRepository chatbotMessageRepository;

    public String processChatMessage(String email, String userMessage) {
        try {
            User user = userRepository.findByEmail(email);
            if (user == null) {
                return "Sorry, I could not find your account. Please login first.";
            }

            String response = generateResponse(user, userMessage);

            ChatbotMessage chatMessage = new ChatbotMessage(user, userMessage, response);
            chatMessage.setMessageType(detectMessageType(userMessage));
            chatbotMessageRepository.save(chatMessage);

            return response;
        } catch (Exception e) {
            return "Sorry, I encountered an error. Please try again.";
        }
    }

    private String generateResponse(User user, String query) {
        String lowerQuery = normalizeQuery(query);

        if (isGreetingRelated(lowerQuery)) {
            return getGreetingResponse(user);
        } else if (isDailyLifeRelated(lowerQuery)) {
            return getDailyLifeResponse(lowerQuery);
        } else if (isDonationRelated(lowerQuery)) {
            return getDonationInfo(user);
        } else if (isCertificateRelated(lowerQuery)) {
            return getCertificateInfo(user);
        } else if (isBloodRequestRelated(lowerQuery)) {
            return getBloodRequestInfo(user);
        } else if (isUserProfileRelated(lowerQuery)) {
            return getUserProfileInfo(user);
        } else if (isEligibilityRelated(lowerQuery)) {
            return checkDonationEligibility(user);
        } else if (isGeneralHelp(lowerQuery)) {
            return getGeneralHelp();
        } else {
            return getSmartResponse(user, lowerQuery);
        }
    }

    private String getDonationInfo(User user) {
        List<Certificate> certificates = certificateRepository.findByDonorUserOrderByCreatedDateDesc(user);

        if (certificates.isEmpty()) {
            return "You have not made any donations yet. Complete your donor profile to get started.";
        }

        int totalDonations = certificates.size();
        int totalUnits = certificates.stream().mapToInt(Certificate::getUnits).sum();

        StringBuilder response = new StringBuilder();
        response.append("Your Donation History\n\n");
        response.append(String.format("Total Donations: %d\n", totalDonations));
        response.append(String.format("Total Units Donated: %d\n\n", totalUnits));
        response.append("Recent Donations:\n");

        certificates.stream().limit(5).forEach(cert -> {
            response.append(String.format("- Donated %d unit(s) at %s on %s\n",
                    cert.getUnits(), cert.getHospitalName(), cert.getDonationDate()));
        });

        return response.toString();
    }

    private String getCertificateInfo(User user) {
        List<Certificate> certificates = certificateRepository.findByDonorUserOrderByCreatedDateDesc(user);

        if (certificates.isEmpty()) {
            return "You do not have any donation certificates yet. Complete a donation to receive one.";
        }

        StringBuilder response = new StringBuilder();
        response.append("Your Certificates\n\n");

        certificates.stream().limit(5).forEach(cert -> {
            response.append(String.format("- Certificate #%s\n", cert.getCertificateNumber()));
            response.append(String.format("  Hospital: %s\n", cert.getHospitalName()));
            response.append(String.format("  Date: %s\n", cert.getDonationDate()));
            response.append(String.format("  Units: %d\n\n", cert.getUnits()));
        });

        return response.toString();
    }

    private String getBloodRequestInfo(User user) {
        List<BloodRequest> requests = bloodRequestRepository.findByUser(user);

        if (requests.isEmpty()) {
            return "You have not created any blood requests yet.";
        }

        StringBuilder response = new StringBuilder();
        response.append("Your Blood Requests\n\n");

        requests.forEach(req -> {
            response.append(String.format("- Patient: %s\n", req.getPatientName()));
            response.append(String.format("  Blood Group: %s\n", req.getBloodGroup()));
            response.append(String.format("  Units Required: %d\n", req.getUnitsRequired()));
            response.append(String.format("  Status: %s\n", req.getStatus()));
            response.append(String.format("  Hospital: %s\n", req.getHospital()));
            response.append(String.format("  Created: %s\n\n", req.getCreatedDate()));
        });

        return response.toString();
    }

    private String getUserProfileInfo(User user) {
        Donor donor = donorRepository.findByUser(user);

        StringBuilder response = new StringBuilder();
        response.append("Your Profile\n\n");
        response.append(String.format("Name: %s\n", user.getName()));
        response.append(String.format("Email: %s\n", user.getEmail()));
        response.append(String.format("Mobile: %s\n", user.getMobile()));

        if (donor != null) {
            response.append("\nDonor Information (Auto-Verified)\n");
            response.append(String.format("Blood Group: %s\n", donor.getBloodGroup()));
            response.append(String.format("Age: %d years\n", donor.getAge()));
            response.append(String.format("Weight: %.1f kg\n", donor.getWeight()));
            response.append(String.format("City: %s\n", donor.getCity()));
            response.append(String.format("Total Donation Units: %d\n", donor.getUnits()));
            if (donor.getLastDonationDate() != null) {
                response.append(String.format("Last Donation: %s\n", donor.getLastDonationDate()));
            } else {
                response.append("Last Donation: Never\n");
            }
        } else {
            response.append("\nDonor Profile Not Started.\n");
            response.append("Complete donor profile to schedule donation and get certificates.");
        }

        return response.toString();
    }

    private String checkDonationEligibility(User user) {
        Donor donor = donorRepository.findByUser(user);

        if (donor == null) {
            return "Donor profile not found. Complete donor profile first to check eligibility.";
        }

        boolean ageEligible = donor.getAge() >= 18 && donor.getAge() <= 65;
        boolean weightEligible = donor.getWeight() >= 50;
        boolean overall = ageEligible && weightEligible;

        StringBuilder response = new StringBuilder();
        response.append("Donation Eligibility Check\n\n");
        response.append(String.format("Age check (%d years): %s\n", donor.getAge(), ageEligible ? "PASS" : "FAIL"));
        response.append(String.format("Weight check (%.1f kg): %s\n", donor.getWeight(), weightEligible ? "PASS" : "FAIL"));
        response.append("\n");

        if (overall) {
            response.append("You are eligible to donate blood.");
        } else {
            response.append("You do not currently meet eligibility criteria.\n");
            if (!ageEligible) response.append("- Age must be 18 to 65 years\n");
            if (!weightEligible) response.append("- Weight must be at least 50 kg\n");
        }

        return response.toString();
    }

    private String getGreetingResponse(User user) {
        String firstName = "there";
        if (user.getName() != null && !user.getName().isBlank()) {
            firstName = user.getName().trim().split("\\s+")[0];
        }
        return "Hello " + firstName + ". I can help with BloodCare information and daily life questions.\n" +
               "Try asking: \"am I eligible\", \"show my certificates\", \"how to sleep better\", \"how to reduce stress\".";
    }

    private String getDailyLifeResponse(String query) {
        if (containsAny(query, "how are you", "kaise ho", "kese ho")) {
            return "I am doing well. I am here to help you with BloodCare and basic daily-life questions.";
        }

        if (containsAny(query, "who are you", "tum kaun ho", "aap kaun ho")) {
            return "I am BloodCare assistant chatbot. I can help with donation info and everyday guidance.";
        }

        if (query.contains("time")) {
            String now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
            return "Current time is " + now + ".";
        }

        if (query.contains("date") || query.contains("day")) {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
            return "Today is " + today + ".";
        }

        if (containsAny(query, "sleep", "tired", "neend", "thaka", "thak")) {
            return "For better sleep:\n" +
                   "1. Keep fixed sleep and wake-up time\n" +
                   "2. Avoid caffeine late evening\n" +
                   "3. Reduce screen time before bed\n" +
                   "4. Keep room dark and cool\n" +
                   "5. Aim for 7-8 hours daily";
        }

        if (containsAny(query, "water", "hydrate", "paani", "pani")) {
            return "Hydration basics:\n" +
                   "1. Start day with one glass of water\n" +
                   "2. Sip water every 60-90 minutes\n" +
                   "3. Increase intake in hot weather and workouts\n" +
                   "4. Keep a bottle near you";
        }

        if (containsAny(query, "stress", "anxiety", "sad", "low", "tanav", "tension", "udaas")) {
            return "Quick stress reset:\n" +
                   "1. Deep breathing for 2 minutes\n" +
                   "2. Short walk or stretch\n" +
                   "3. Do one small task first\n" +
                   "4. Talk to someone you trust\n" +
                   "If stress feels severe or daily, please consult a professional.";
        }

        if (containsAny(query, "diet", "food", "khana", "meal")) {
            return "Balanced diet basics:\n" +
                   "1. Add protein in each meal\n" +
                   "2. Include fruits and vegetables daily\n" +
                   "3. Reduce sugary drinks and junk food\n" +
                   "4. Prefer home-cooked meals";
        }

        if (containsAny(query, "exercise", "workout", "fitness", "walk", "yoga")) {
            return "Simple fitness plan:\n" +
                   "1. 30 minutes brisk walk, 5 days/week\n" +
                   "2. Strength training 2-3 days/week\n" +
                   "3. Stretching 5-10 minutes daily";
        }

        if (containsAny(query, "focus", "study", "routine", "productivity", "padhai", "concentrate")) {
            return "Focus routine:\n" +
                   "1. Pick top one priority\n" +
                   "2. Work in 25 min focus + 5 min break cycles\n" +
                   "3. Keep phone away in focus time\n" +
                   "4. Plan tomorrow in 5 minutes before sleep";
        }

        if (containsAny(query, "headache", "head pain", "sir dard")) {
            return "For mild headache: hydrate, rest eyes, and take a short break.\n" +
                   "If severe or frequent headache continues, consult a doctor.";
        }

        if (containsAny(query, "cold", "cough", "fever", "bukhar")) {
            return "For mild cold/fever: rest, hydrate, and eat light food.\n" +
                   "If fever is high, lasts more than 2 days, or symptoms worsen, consult a doctor.";
        }

        if (containsAny(query, "money", "budget", "save money", "bachat")) {
            return "Simple money routine:\n" +
                   "1. Track expenses for 7 days\n" +
                   "2. Use 50-30-20 rule (needs-wants-savings)\n" +
                   "3. Set one monthly savings goal\n" +
                   "4. Avoid impulse purchases";
        }

        return "I can answer daily life topics like sleep, stress, water, diet, exercise and routine. Ask in simple words.";
    }

    private String getSmartResponse(User user, String lowerQuery) {
        if (containsAny(lowerQuery, "bye", "by", "goodbye", "see you", "tata", "alvida")) {
            return "Goodbye. Take care and come back anytime if you need help.";
        }

        if (containsAny(lowerQuery, "thank", "thanks", "shukriya")) {
            return "You are welcome. Happy to help.";
        }

        if (lowerQuery.contains("how") && lowerQuery.contains("donate")) {
            return "To donate blood:\n" +
                   "1. Complete donor profile\n" +
                   "2. Check eligibility\n" +
                   "3. Schedule visit\n" +
                   "4. Visit hospital on date\n" +
                   "5. Receive certificate";
        }

        if (lowerQuery.contains("help")) {
            return getGeneralHelp();
        }

        return "I can help with:\n" +
               "- Donation history\n" +
               "- Certificates\n" +
               "- Blood requests\n" +
               "- Profile and eligibility\n" +
               "- Daily life questions (sleep, stress, routine, water, diet)\n\n" +
               "What do you want to know?";
    }

    private String getGeneralHelp() {
        return "BloodCare Chatbot Help\n\n" +
               "Donation:\n" +
               "- show my donation history\n" +
               "- how many donations\n\n" +
               "Certificates:\n" +
               "- show my certificates\n\n" +
               "Blood Requests:\n" +
               "- show my blood requests\n\n" +
               "Profile:\n" +
               "- show my profile\n" +
               "- am I eligible to donate\n\n" +
               "Daily life:\n" +
               "- how to reduce stress\n" +
               "- sleep routine\n" +
               "- hydration tips\n" +
               "- focus and productivity tips";
    }

    private boolean isDonationRelated(String query) {
        return containsAny(query, "donation", "donated", "how many", "donate", "rakt daan", "blood donate");
    }

    private boolean isCertificateRelated(String query) {
        return containsAny(query, "certificate", "cert", "praman patra");
    }

    private boolean isBloodRequestRelated(String query) {
        return containsAny(query, "blood request", "request blood", "need blood", "blood chahiye", "urgent blood");
    }

    private boolean isUserProfileRelated(String query) {
        return containsAny(query, "profile", "my information", "my details", "who am i", "meri details", "mera profile");
    }

    private boolean isEligibilityRelated(String query) {
        return containsAny(query, "eligible", "elegibility", "can i donate", "requirements", "kya donate kar sakta", "eligibility");
    }

    private boolean isGeneralHelp(String query) {
        return containsAny(query, "help", "what can you do", "what can i ask", "madad", "kya puchu");
    }

    private boolean isGreetingRelated(String query) {
        return query.equals("hi") || query.equals("hii") || query.equals("hy") ||
               query.equals("hello") || query.equals("hey") ||
               containsAny(query, "good morning", "good afternoon", "good evening", "namaste", "salam");
    }

    private boolean isDailyLifeRelated(String query) {
        return containsAny(query,
                "time", "date", "day", "sleep", "water", "hydrate", "diet", "food",
                "exercise", "workout", "stress", "anxiety", "sad", "motivation",
                "focus", "routine", "study", "productivity", "headache", "tired",
                "lifestyle", "neend", "paani", "pani", "khana", "tanav", "tension",
                "padhai", "sir dard", "bukhar", "cold", "cough", "budget", "money",
                "how are you", "who are you");
    }

    private String detectMessageType(String message) {
        String lower = normalizeQuery(message);
        if (isGreetingRelated(lower)) return "GREETING";
        if (isDailyLifeRelated(lower)) return "DAILY_LIFE";
        if (isDonationRelated(lower)) return "DONATION";
        if (isCertificateRelated(lower)) return "CERTIFICATE";
        if (isBloodRequestRelated(lower)) return "BLOOD_REQUEST";
        if (isUserProfileRelated(lower)) return "USER_INFO";
        if (isEligibilityRelated(lower)) return "ELIGIBILITY";
        return "GENERAL";
    }

    public List<ChatbotMessage> getChatHistory(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return chatbotMessageRepository.findByUserOrderByTimestampAsc(user);
        }
        return List.of();
    }

    private String normalizeQuery(String input) {
        if (input == null) {
            return "";
        }

        String normalized = input.toLowerCase(Locale.ROOT).trim();
        return normalized.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String source, String... tokens) {
        for (String token : tokens) {
            if (source.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
