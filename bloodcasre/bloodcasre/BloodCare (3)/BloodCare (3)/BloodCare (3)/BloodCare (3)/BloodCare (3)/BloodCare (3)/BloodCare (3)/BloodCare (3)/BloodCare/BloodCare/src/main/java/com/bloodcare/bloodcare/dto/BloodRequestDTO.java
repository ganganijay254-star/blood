package com.bloodcare.bloodcare.dto;

import com.bloodcare.bloodcare.entity.BloodRequest;
import java.time.LocalDateTime;

public class BloodRequestDTO {

    private Long id;
    private String publicId;
    private String patientName;
    private String bloodGroup;
    private int unitsRequired;
    private String hospital;
    private String city;
    private String contactNumber;
    private String urgency;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime approvedDate;
    private LocalDateTime fulfilledDate;
    private String expectedDeliveryTime;
    private String deliveryLocation;
    private int matchedDonorsCount;
    private int donorsRespondedCount;
    private boolean verified;

    public static BloodRequestDTO fromEntity(BloodRequest request) {
        BloodRequestDTO dto = new BloodRequestDTO();
        dto.id = request.getId();
        dto.publicId = request.getPublicId();
        dto.patientName = request.getPatientName();
        dto.bloodGroup = request.getBloodGroup();
        dto.unitsRequired = request.getUnitsRequired();
        dto.hospital = request.getHospital();
        dto.city = request.getCity();
        dto.contactNumber = request.getContactNumber();
        dto.urgency = request.getUrgency();
        dto.status = request.getStatus();
        dto.createdDate = request.getCreatedDate();
        dto.approvedDate = request.getApprovedDate();
        dto.fulfilledDate = request.getFulfilledDate();
        dto.expectedDeliveryTime = request.getExpectedDeliveryTime();
        dto.deliveryLocation = request.getDeliveryLocation();
        dto.matchedDonorsCount = request.getMatchedDonorsCount();
        dto.donorsRespondedCount = request.getDonorsRespondedCount();
        dto.verified = request.isVerified();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public String getHospital() {
        return hospital;
    }

    public String getCity() {
        return city;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public LocalDateTime getFulfilledDate() {
        return fulfilledDate;
    }

    public String getExpectedDeliveryTime() {
        return expectedDeliveryTime;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public int getMatchedDonorsCount() {
        return matchedDonorsCount;
    }

    public int getDonorsRespondedCount() {
        return donorsRespondedCount;
    }

    public boolean isVerified() {
        return verified;
    }
}
