package com.bloodcare.bloodcare.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.UserRepository;
import com.bloodcare.bloodcare.service.EmailService;
import com.bloodcare.bloodcare.dto.LoginRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /* =====================
       SIGNUP (USER ONLY)
    ===================== */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body("Email already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        savedUser.setPassword(null);

        return ResponseEntity.ok(savedUser);
    }

    /* =====================
       LOGIN (USER ONLY)
    ===================== */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpSession session) {

        User user = userRepository.findByEmail(request.getEmail());

        if (user == null ||
            !passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            return ResponseEntity.status(401).body("Invalid credentials");
        }
        
        if (user.isBlocked()) {
            if (user.isBlockedByAdmin()) {
                return ResponseEntity.status(403).body("Your account has been blocked by admin");
            }
            user.setBlocked(false);
            user.setBlockedByAdmin(false);
            userRepository.save(user);
        }

        user.setPassword(null);
        session.setAttribute("user", user);

        return ResponseEntity.ok(user);
    }

    /* =====================
       USER SESSION CHECK
    ===================== */
    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        
        User latestUser = userRepository.findByEmail(user.getEmail());
        if (latestUser == null) {
            session.removeAttribute("user");
            return ResponseEntity.status(401).body("Not logged in");
        }

        if (latestUser.isBlocked()) {
            if (latestUser.isBlockedByAdmin()) {
                session.removeAttribute("user");
                return ResponseEntity.status(401).body("Not logged in");
            }
            latestUser.setBlocked(false);
            latestUser.setBlockedByAdmin(false);
            latestUser = userRepository.save(latestUser);
        }
        
        latestUser.setPassword(null);

        return ResponseEntity.ok(latestUser);
    }

    /* =====================
       USER LOGOUT
    ===================== */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {

        session.removeAttribute("user");
        return ResponseEntity.ok("Logged out successfully");
    }

    /* =====================
       FORGOT PASSWORD
    ===================== */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam String email,
            HttpServletRequest request) {

        User user = userRepository.findByEmail(email);

        if (user == null) {
            return ResponseEntity.badRequest().body("Email not registered");
        }

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);

        // Build reset link from request
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();
        
        String baseUrl = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl += ":" + serverPort;
        }
        
        String resetLink = baseUrl + "/reset-password.html?token=" + token;

        emailService.sendResetLink(user.getEmail(), resetLink);

        return ResponseEntity.ok("Reset link sent to email");
    }

    /* =====================
       RESET PASSWORD
    ===================== */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {

        String token = req.get("token");
        String newPassword = req.get("password");

        User user = userRepository.findByResetToken(token);

        if (user == null) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);

        userRepository.save(user);

        return ResponseEntity.ok("Password updated successfully");
    }
}
