package com.bloodcare.bloodcare.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;

@Entity
@Table(name = "blood_request_responses")
public class BloodRequestResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "blood_request_id")
    private BloodRequest bloodRequest;

    @ManyToOne
    @JoinColumn(name = "donor_id")
    private Donor donor;

    private String status; // PENDING, ACCEPTED, DECLINED

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime responseDate;

    public BloodRequestResponse() {}

    public BloodRequestResponse(BloodRequest bloodRequest, Donor donor, String status) {
        this.bloodRequest = bloodRequest;
        this.donor = donor;
        this.status = status;
        this.responseDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public BloodRequest getBloodRequest() { return bloodRequest; }
    public void setBloodRequest(BloodRequest bloodRequest) { this.bloodRequest = bloodRequest; }
    public Donor getDonor() { return donor; }
    public void setDonor(Donor donor) { this.donor = donor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }
}
