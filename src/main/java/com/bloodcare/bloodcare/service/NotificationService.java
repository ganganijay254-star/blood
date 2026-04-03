package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.Notification;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Create a simple notification
     */
    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        return notificationRepository.save(notification);
    }

    /**
     * Create a blood delivery notification with details
     */
    public Notification createBloodDeliveryNotification(User user, String bloodGroup, 
                                                        Integer unitsDelivered, String hospitalName,
                                                        String location, String expectedDeliveryTime) {
        String title = "🩸 Blood Delivery Confirmed";
        String message = String.format("Your %s blood (%d units) will arrive at %s within %s", 
                                      bloodGroup, unitsDelivered, hospitalName, expectedDeliveryTime);
        
        Notification notification = new Notification(user, title, message, "BLOOD_DELIVERY",
                                                    bloodGroup, unitsDelivered, hospitalName,
                                                    location, expectedDeliveryTime);
        return notificationRepository.save(notification);
    }

    /**
     * Get all notifications for a user
     */
    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedDateDesc(user);
    }

    /**
     * Get unread notifications for a user
     */
    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedDateDesc(user);
    }

    /**
     * Mark notification as read
     */
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return null;
    }

    /**
     * Mark all notifications as read for a user
     */
    public void markAllAsRead(User user) {
        List<Notification> unreadNotifications = getUnreadNotifications(user);
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    /**
     * Get unread notification count
     */
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Delete a notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Get notifications by type
     */
    public List<Notification> getNotificationsByType(User user, String type) {
        return notificationRepository.findByUserAndTypeOrderByCreatedDateDesc(user, type);
    }
}
