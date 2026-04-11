package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.entity.BloodBank;
import com.bloodcare.bloodcare.service.BloodBankService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blood-banks")
public class BloodBankController {
    private final BloodBankService bloodBankService;

    public BloodBankController(BloodBankService bloodBankService) {
        this.bloodBankService = bloodBankService;
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(
            @RequestParam String name,
            @RequestParam String location,
            @RequestParam String bloodGroup,
            @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(bloodBankService.upsert(name, location, bloodGroup, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<BloodBank>> list() {
        return ResponseEntity.ok(bloodBankService.findAll());
    }
}
