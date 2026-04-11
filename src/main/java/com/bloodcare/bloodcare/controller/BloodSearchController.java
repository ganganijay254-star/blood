package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.service.BloodStockService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blood")
public class BloodSearchController {
    private final BloodStockService bloodStockService;

    public BloodSearchController(BloodStockService bloodStockService) {
        this.bloodStockService = bloodStockService;
    }

    @GetMapping
    public ResponseEntity<?> getBlood(@RequestParam(value = "group", required = false) String group) {
        if (group != null && !group.isBlank()) {
            return ResponseEntity.ok(bloodStockService.searchAvailableBlood(group));
        }
        return ResponseEntity.ok(bloodStockService.getAllStock());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam("group") String group) {
        return ResponseEntity.ok(bloodStockService.searchAvailableBlood(group));
    }

    @PostMapping
    public ResponseEntity<?> createBlood(@RequestBody Map<String, Object> payload) {
        try {
            Long hospitalId = Long.valueOf(String.valueOf(payload.get("hospitalId")));
            String bloodGroup = String.valueOf(payload.get("bloodGroup"));
            int unitsAvailable = Integer.parseInt(String.valueOf(payload.getOrDefault("unitsAvailable", 0)));
            Integer reorderLevel = payload.get("reorderLevel") == null
                    ? null
                    : Integer.valueOf(String.valueOf(payload.get("reorderLevel")));

            BloodStock stock = bloodStockService.createStock(hospitalId, bloodGroup, unitsAvailable, reorderLevel);
            return ResponseEntity.status(HttpStatus.CREATED).body(stock);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBlood(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Long hospitalId = payload.get("hospitalId") == null
                    ? null
                    : Long.valueOf(String.valueOf(payload.get("hospitalId")));
            String bloodGroup = payload.get("bloodGroup") == null
                    ? null
                    : String.valueOf(payload.get("bloodGroup"));
            int unitsAvailable = Integer.parseInt(String.valueOf(payload.getOrDefault("unitsAvailable", 0)));
            Integer reorderLevel = payload.get("reorderLevel") == null
                    ? null
                    : Integer.valueOf(String.valueOf(payload.get("reorderLevel")));

            BloodStock stock = bloodStockService.updateStock(id, hospitalId, bloodGroup, unitsAvailable, reorderLevel);
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBlood(@PathVariable Long id) {
        try {
            bloodStockService.deleteStock(id);
            return ResponseEntity.ok(Map.of("message", "Blood stock deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
