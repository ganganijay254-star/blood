package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.UserRepository;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    /* ===== UPLOAD PROFILE PHOTO ===== */
    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @RequestParam String photoBase64,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // Validate base64
            if (!photoBase64.startsWith("data:image")) {
                response.put("success", false);
                response.put("message", "Invalid image format");
                return ResponseEntity.badRequest().body(response);
            }

            String[] parts = photoBase64.split(",", 2);
            if (parts.length != 2) {
                response.put("success", false);
                response.put("message", "Invalid image payload");
                return ResponseEntity.badRequest().body(response);
            }

            String metadata = parts[0];
            String base64Data = parts[1];

            String extension = "jpg";
            if (metadata.contains("image/png")) {
                extension = "png";
            } else if (metadata.contains("image/webp")) {
                extension = "webp";
            } else if (metadata.contains("image/gif")) {
                extension = "gif";
            }

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            Path uploadDir = Paths.get("uploads", "profile-photos");
            Files.createDirectories(uploadDir);

            if (user.getProfilePhoto() != null && user.getProfilePhoto().startsWith("/uploads/profile-photos/")) {
                try {
                    String oldFileName = user.getProfilePhoto().substring("/uploads/profile-photos/".length());
                    Files.deleteIfExists(uploadDir.resolve(oldFileName));
                } catch (Exception ignored) {
                }
            }

            String fileName = "user-" + user.getId() + "-" + UUID.randomUUID() + "." + extension;
            Files.write(uploadDir.resolve(fileName), imageBytes);

            user.setProfilePhoto("/uploads/profile-photos/" + fileName);
            User updated = userRepository.save(user);
            
            // Update session
            session.setAttribute("user", updated);
            
            response.put("success", true);
            response.put("message", "Profile photo updated successfully");
            response.put("user", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error uploading photo: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /* ===== GET PROFILE PHOTO ===== */
    @GetMapping("/profile-photo/{userId}")
    public ResponseEntity<?> getProfilePhoto(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("photo", user.getProfilePhoto());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching photo");
        }
    }

    /* ===== UPDATE USER PROFILE ===== */
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> updates,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.status(401).body(response);
        }

        try {
            if (updates.containsKey("name")) {
                user.setName(updates.get("name"));
            }
            if (updates.containsKey("mobile")) {
                user.setMobile(updates.get("mobile"));
            }
            
            User updated = userRepository.save(user);
            
            // Update session
            session.setAttribute("user", updated);
            
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", updated);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating profile: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /* ===== GET USER PROFILE ===== */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        Map<String, Object> response = new HashMap<>();
        
        if (user == null) {
            response.put("success", false);
            response.put("message", "Please login first");
            return ResponseEntity.status(401).body(response);
        }

        try {
            User freshUser = userRepository.findById(user.getId()).orElse(null);
            
            if (freshUser == null) {
                response.put("success", false);
                response.put("message", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(freshUser);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error fetching profile");
            return ResponseEntity.status(500).body(response);
        }
    }
}
