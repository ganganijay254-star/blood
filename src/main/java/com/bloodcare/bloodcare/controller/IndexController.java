package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.DonorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    private final DonorRepository donorRepository;

    public IndexController(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";   // ❌ no .html
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
    @GetMapping("/donor-profile")
    public String donorProfile() {
        return "donor-profile";   // .html nahi likhna
    }
    
    @GetMapping("/medicine")
    public String medicine() {
        return "medicine";   // ❌ .html mat likhna
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/admin")
    public String adminLogin() {
        return "admin";
    }
   

    @GetMapping("/admin-dashboard")
    public String adminDashboard() {
        return "admin-dashboard";
    }
    
    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/donor-form")
    public String donorForm(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null && donorRepository.findByUser(user) != null) {
            return "redirect:/donor-dashboard";
        }
        return "donor-form";
    }

    @GetMapping("/chatbot")
    public String chatbot() {
        return "chatbot";
    }

    @GetMapping("/donor-dashboard")
    public String donorDashboard() {
        return "donor-dashboard";
    }

    @GetMapping("/visit")
    public String visit() {
        return "visit";
    }
    @GetMapping("/request-visit")
    public String requestvisit() {
        return "request-visit";
    }
    @GetMapping("/leaderboard")
    public String Leaderboard() {
        return "leaderboard";
    }

    @GetMapping("/receiver-form")
    public String receiverForm() {
        return "receiver-form";
    }

    @GetMapping("/receiver-track")
    public String receiverTrack() {
        return "receiver-track";
    }

    @GetMapping("/blood-request")
    public String bloodRequest() {
        return "blood-request";
    }

    @GetMapping("/visit-history")
    public String visitHistory() {
        return "visit-history";
    }

}
