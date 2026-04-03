package com.bloodcare.bloodcare.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String patientName;
    private String bloodGroup;        // A+, A-, B+, B-, AB+, AB-, O+, O-
    private int unitsRequired;
    private String hospital;
    private String city;
    private String contactNumber;
    
    private String urgency;           // LOW, MEDIUM, HIGH, CRITICAL
    private String description;       // Medical reason, details
    
    private String status;            // PENDING_APPROVAL, OPEN, RESERVED, IN_PROGRESS, DONOR_ASSIGNED, BLOOD_BANK_AVAILABLE, FAILED, COMPLETED, CANCELLED
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fulfilledDate;
    
    // Delivery notification fields
    private String expectedDeliveryTime;  // e.g., "2-3 hours", "Today by 6 PM"
    private String deliveryLocation;     // Wing/branch info
    private String deliveryInstructions; // Special instructions
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvedDate;
    
    @ManyToOne
    @JoinColumn(name = "confirmed_donor_id")
    private Donor confirmedDonor;  // Assigned donor for this request (DB column: confirmed_donor_id)

    // Public identifier for UI e.g., BR-1023
    private String publicId;

    // Tracking fields
    private int matchedDonorsCount = 0;
    private int donorsRespondedCount = 0;

    // Whether admin has verified this request (controls public visibility)
    private boolean verified = false;

    // Path to uploaded medical proof (relative URL under /uploads/medical-proofs)
    private String medicalProofPath;

    /* ===== CONSTRUCTORS ===== */
    public BloodRequest() {
    }

    public BloodRequest(User user, String patientName, String bloodGroup, 
                       int unitsRequired, String hospital, String city, 
                       String contactNumber, String urgency, String description) {
        this.user = user;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospital = hospital;
        this.city = city;
        this.contactNumber = contactNumber;
        this.urgency = urgency;
        this.description = description;
        this.status = "OPEN";
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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public LocalDateTime getFulfilledDate() {
        return fulfilledDate;
    }

    public void setFulfilledDate(LocalDateTime fulfilledDate) {
        this.fulfilledDate = fulfilledDate;
    }

    public String getExpectedDeliveryTime() {
        return expectedDeliveryTime;
    }

    public void setExpectedDeliveryTime(String expectedDeliveryTime) {
        this.expectedDeliveryTime = expectedDeliveryTime;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getDeliveryInstructions() {
        return deliveryInstructions;
    }

    public void setDeliveryInstructions(String deliveryInstructions) {
        this.deliveryInstructions = deliveryInstructions;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public Donor getConfirmedDonor() {
        return confirmedDonor;
    }

    public void setConfirmedDonor(Donor confirmedDonor) {
        this.confirmedDonor = confirmedDonor;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public int getMatchedDonorsCount() {
        return matchedDonorsCount;
    }

    public void setMatchedDonorsCount(int matchedDonorsCount) {
        this.matchedDonorsCount = matchedDonorsCount;
    }

    public int getDonorsRespondedCount() {
        return donorsRespondedCount;
    }

    public void setDonorsRespondedCount(int donorsRespondedCount) {
        this.donorsRespondedCount = donorsRespondedCount;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getMedicalProofPath() {
        return medicalProofPath;
    }

    public void setMedicalProofPath(String medicalProofPath) {
        this.medicalProofPath = medicalProofPath;
    }
}
