package com.bloodcare.bloodcare.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;

@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "visit_request_id")
    private VisitRequest visitRequest;  // Track which visit this reward is from

    private String title;           // e.g., "Free Health Checkup"
    private String description;     // Description of reward
    private String rewardCode;      // Unique code for redemption
    private String category;        // "health", "discount", "badge"
    private String icon;            // Emoji or icon
    private int value;              // Value/Points

    private String status;          // ACTIVE, REDEEMED, EXPIRED
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime redeemedDate;

    /* ===== CONSTRUCTORS ===== */
    public Reward() {
    }

    public Reward(User user, String title, String description, String rewardCode, 
                  String category, String icon, int value) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.rewardCode = rewardCode;
        this.category = category;
        this.icon = icon;
        this.value = value;
        this.status = "ACTIVE";
        this.createdDate = LocalDateTime.now();
    }

    /* ===== GETTERS & SETTERS ===== */

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

    public VisitRequest getVisitRequest() {
        return visitRequest;
    }

    public void setVisitRequest(VisitRequest visitRequest) {
        this.visitRequest = visitRequest;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRewardCode() {
        return rewardCode;
    }

    public void setRewardCode(String rewardCode) {
        this.rewardCode = rewardCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getRedeemedDate() {
        return redeemedDate;
    }

    public void setRedeemedDate(LocalDateTime redeemedDate) {
        this.redeemedDate = redeemedDate;
    }
}
