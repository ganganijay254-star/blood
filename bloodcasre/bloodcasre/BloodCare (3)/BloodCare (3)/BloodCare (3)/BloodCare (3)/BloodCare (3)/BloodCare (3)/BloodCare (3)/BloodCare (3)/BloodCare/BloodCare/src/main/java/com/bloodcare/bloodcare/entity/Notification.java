package com.bloodcare.bloodcare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;
    private String message;
    private String type;  // BLOOD_DELIVERY, BLOOD_REQUEST_APPROVED, GENERAL, etc.
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;
    
    private boolean isRead = false;
    
    // Additional details for blood delivery notifications
    private String bloodGroup;
    private Integer unitsDelivered;
    private String hospitalName;
    private String location;
    private String expectedDeliveryTime;  // e.g., "2-3 hours", "Today by 6 PM"
    
    // =====================
    // CONSTRUCTORS
    // =====================
    
    public Notification() {
    }

    public Notification(User user, String title, String message, String type) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdDate = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(User user, String title, String message, String type, 
                       String bloodGroup, Integer unitsDelivered, String hospitalName,
                       String location, String expectedDeliveryTime) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type;
        this.bloodGroup = bloodGroup;
        this.unitsDelivered = unitsDelivered;
        this.hospitalName = hospitalName;
        this.location = location;
        this.expectedDeliveryTime = expectedDeliveryTime;
        this.createdDate = LocalDateTime.now();
        this.isRead = false;
    }

    // =====================
    // GETTERS & SETTERS
    // =====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public Integer getUnitsDelivered() {
        return unitsDelivered;
    }

    public void setUnitsDelivered(Integer unitsDelivered) {
        this.unitsDelivered = unitsDelivered;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getExpectedDeliveryTime() {
        return expectedDeliveryTime;
    }

    public void setExpectedDeliveryTime(String expectedDeliveryTime) {
        this.expectedDeliveryTime = expectedDeliveryTime;
    }
}
