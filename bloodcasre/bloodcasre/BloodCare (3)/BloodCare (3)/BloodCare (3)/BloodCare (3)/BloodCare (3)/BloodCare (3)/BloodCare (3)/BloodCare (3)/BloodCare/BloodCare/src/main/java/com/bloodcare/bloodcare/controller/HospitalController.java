package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.dto.HospitalDTO;
import com.bloodcare.bloodcare.service.HospitalService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    @Autowired
    private HospitalService hospitalService;

    @GetMapping("/nearby")
    public List<HospitalDTO> getNearby(
            @RequestParam double lat,
            @RequestParam double lng) {

        return hospitalService.findNearby(lat, lng);
    }
}