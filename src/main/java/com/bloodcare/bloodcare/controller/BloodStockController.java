package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.service.BloodStockService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital-stock")
public class BloodStockController {
    private final BloodStockService bloodStockService;

    public BloodStockController(BloodStockService bloodStockService) {
        this.bloodStockService = bloodStockService;
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(
            @RequestParam Long hospitalId,
            @RequestParam String bloodGroup,
            @RequestParam int unitsAvailable,
            @RequestParam(required = false) Integer reorderLevel) {
        try {
            BloodStock stock = bloodStockService.upsertStock(hospitalId, bloodGroup, unitsAvailable, reorderLevel);
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<BloodStock>> hospitalStock(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(bloodStockService.getHospitalStock(hospitalId));
    }

    @GetMapping("/low-alerts")
    public ResponseEntity<List<BloodStock>> lowStockAlerts() {
        return ResponseEntity.ok(bloodStockService.lowStockAlerts());
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(bloodStockService.overview());
    }
}
