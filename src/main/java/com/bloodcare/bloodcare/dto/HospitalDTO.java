package com.bloodcare.bloodcare.dto;

public class HospitalDTO {

    private Long id;
    private String name;
    private String address;
    private double distance;

    // Constructor
    public HospitalDTO(Long id, String name, String address, double distance) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.distance = distance;
    }

    // Getters only (DTO usually immutable)
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getDistance() {
        return distance;
    }
}