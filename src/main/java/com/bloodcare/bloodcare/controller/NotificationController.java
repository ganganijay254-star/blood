package com.bloodcare.bloodcare.controller;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bloodcare.bloodcare.entity.Notification;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /* ===== GET ALL NOTIFICATIONS ===== */
    @GetMapping("/all")
    public ResponseEntity<?> getAllNotifications(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            List<Notification> notifications = notificationService.getNotifications(user);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching notifications: " + e.getMessage());
        }
    }

    /* ===== GET UNREAD NOTIFICATIONS ===== */
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            List<Notification> notifications = notificationService.getUnreadNotifications(user);
            long unreadCount = notificationService.getUnreadCount(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications);
            response.put("unreadCount", unreadCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching unread notifications: " + e.getMessage());
        }
    }

    /* ===== GET UNREAD COUNT ===== */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            long unreadCount = notificationService.getUnreadCount(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", unreadCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching unread count: " + e.getMessage());
        }
    }

    /* ===== MARK NOTIFICATION AS READ ===== */
    @PostMapping("/mark-read/{notificationId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            Notification notification = notificationService.markAsRead(notificationId);
            
            if (notification == null) {
                return ResponseEntity.badRequest().body("Notification not found");
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("notification", notification);
            response.put("message", "Notification marked as read");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error marking notification as read: " + e.getMessage());
        }
    }

    /* ===== MARK ALL NOTIFICATIONS AS READ ===== */
    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            notificationService.markAllAsRead(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All notifications marked as read");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error marking notifications as read: " + e.getMessage());
        }
    }

    /* ===== DELETE NOTIFICATION ===== */
    @DeleteMapping("/delete/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            notificationService.deleteNotification(notificationId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification deleted");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting notification: " + e.getMessage());
        }
    }

    /* ===== GET BLOOD DELIVERY NOTIFICATIONS ===== */
    @GetMapping("/blood-deliveries")
    public ResponseEntity<?> getBloodDeliveryNotifications(HttpSession session) {
        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            return ResponseEntity.status(401).body("Please login first");
        }

        try {
            List<Notification> notifications = notificationService.getNotificationsByType(user, "BLOOD_DELIVERY");
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error fetching blood delivery notifications: " + e.getMessage());
        }
    }
}
