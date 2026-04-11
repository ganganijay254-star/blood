package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.entity.BloodBank;
import com.bloodcare.bloodcare.entity.Hospital;
import com.bloodcare.bloodcare.repository.BloodBankRepository;
import com.bloodcare.bloodcare.repository.BloodStockRepository;
import com.bloodcare.bloodcare.repository.HospitalRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class BloodStockService {
    private static final Map<String, List<String>> COMPATIBILITY_MAP = createCompatibilityMap();

    private final BloodStockRepository bloodStockRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;

    public BloodStockService(BloodStockRepository bloodStockRepository, HospitalRepository hospitalRepository,
                             BloodBankRepository bloodBankRepository) {
        this.bloodStockRepository = bloodStockRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodBankRepository = bloodBankRepository;
    }

    public BloodStock upsertStock(Long hospitalId, String bloodGroup, int unitsAvailable, Integer reorderLevel) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        BloodStock stock = bloodStockRepository.findByHospitalIdAndBloodGroupIgnoreCase(hospitalId, bloodGroup)
                .orElseGet(BloodStock::new);

        stock.setHospital(hospital);
        stock.setBloodGroup(bloodGroup == null ? "" : bloodGroup.trim().toUpperCase());
        stock.setUnitsAvailable(Math.max(0, unitsAvailable));
        if (reorderLevel != null && reorderLevel >= 0) {
            stock.setReorderLevel(reorderLevel);
        }
        stock.setLastUpdated(LocalDateTime.now());
        return bloodStockRepository.save(stock);
    }

    public BloodStock createStock(Long hospitalId, String bloodGroup, int unitsAvailable, Integer reorderLevel) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        BloodStock stock = new BloodStock();
        stock.setHospital(hospital);
        stock.setBloodGroup(bloodGroup == null ? "" : bloodGroup.trim().toUpperCase());
        stock.setUnitsAvailable(Math.max(0, unitsAvailable));
        stock.setReorderLevel(reorderLevel != null && reorderLevel >= 0 ? reorderLevel : 5);
        stock.setLastUpdated(LocalDateTime.now());
        return bloodStockRepository.save(stock);
    }

    public BloodStock updateStock(Long id, Long hospitalId, String bloodGroup, int unitsAvailable, Integer reorderLevel) {
        BloodStock stock = bloodStockRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blood stock not found"));

        if (hospitalId != null) {
            Hospital hospital = hospitalRepository.findById(hospitalId)
                    .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));
            stock.setHospital(hospital);
        }

        if (bloodGroup != null && !bloodGroup.isBlank()) {
            stock.setBloodGroup(bloodGroup.trim().toUpperCase());
        }

        stock.setUnitsAvailable(Math.max(0, unitsAvailable));
        if (reorderLevel != null && reorderLevel >= 0) {
            stock.setReorderLevel(reorderLevel);
        }
        stock.setLastUpdated(LocalDateTime.now());
        return bloodStockRepository.save(stock);
    }

    public void deleteStock(Long id) {
        if (!bloodStockRepository.existsById(id)) {
            throw new IllegalArgumentException("Blood stock not found");
        }
        bloodStockRepository.deleteById(id);
    }

    public List<BloodStock> getAllStock() {
        return bloodStockRepository.findAll();
    }

    public List<BloodStock> getHospitalStock(Long hospitalId) {
        return bloodStockRepository.findByHospitalId(hospitalId);
    }

    public List<BloodStock> lowStockAlerts() {
        return bloodStockRepository.findAll().stream()
                .filter(stock -> stock.getUnitsAvailable() <= stock.getReorderLevel())
                .toList();
    }

    public Map<String, Object> overview() {
        List<BloodStock> all = bloodStockRepository.findAll();
        int totalUnits = all.stream().mapToInt(BloodStock::getUnitsAvailable).sum();
        long lowStockCount = all.stream().filter(s -> s.getUnitsAvailable() <= s.getReorderLevel()).count();
        Map<String, Object> result = new HashMap<>();
        result.put("records", all.size());
        result.put("totalUnits", totalUnits);
        result.put("lowStockCount", lowStockCount);
        result.put("stock", all);
        return result;
    }

    public List<Map<String, Object>> searchAvailableBlood(String bloodGroup) {
        List<BloodStock> stocks = bloodStockRepository
                .findByBloodGroupIgnoreCaseAndUnitsAvailableGreaterThan(bloodGroup, 0);

        List<Map<String, Object>> results = new ArrayList<>();
        for (BloodStock stock : stocks) {
            if (stock.getHospital() == null) {
                continue;
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("hospitalId", stock.getHospital().getId());
            row.put("hospitalName", stock.getHospital().getName());
            row.put("location", stock.getHospital().getAddress());
            row.put("contact", stock.getHospital().getContact());
            row.put("bloodGroup", stock.getBloodGroup());
            row.put("unitsAvailable", stock.getUnitsAvailable());
            row.put("availability", stock.getUnitsAvailable() > 0 ? "AVAILABLE" : "NOT_AVAILABLE");
            results.add(row);
        }

        return results;
    }

    public List<String> getCompatibleBloodGroups(String receiverBloodGroup) {
        return COMPATIBILITY_MAP.getOrDefault(normalizeBloodGroup(receiverBloodGroup), Collections.emptyList());
    }

    public Map<String, Object> getHospitalStockSnapshot(String hospitalName, String receiverBloodGroup) {
        Map<String, Object> result = new HashMap<>();
        String normalizedGroup = normalizeBloodGroup(receiverBloodGroup);
        List<String> compatibleGroups = getCompatibleBloodGroups(normalizedGroup);
        result.put("requestedBloodGroup", normalizedGroup);
        result.put("compatibleGroups", compatibleGroups);

        Optional<Hospital> hospitalOptional = findHospitalByName(hospitalName);
        if (hospitalOptional.isEmpty()) {
            result.put("hospitalFound", false);
            result.put("hospitalName", hospitalName);
            result.put("availableUnits", 0);
            result.put("stock", List.of());
            return result;
        }

        Hospital hospital = hospitalOptional.get();
        List<Map<String, Object>> stockRows = new ArrayList<>();
        int totalCompatibleUnits = 0;

        for (String bloodGroup : compatibleGroups) {
            BloodStock stock = bloodStockRepository.findByHospitalIdAndBloodGroupIgnoreCase(hospital.getId(), bloodGroup).orElse(null);
            int unitsAvailable = stock == null ? 0 : stock.getUnitsAvailable();
            totalCompatibleUnits += unitsAvailable;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bloodGroup", bloodGroup);
            row.put("unitsAvailable", unitsAvailable);
            row.put("reorderLevel", stock == null ? 0 : stock.getReorderLevel());
            row.put("lastUpdated", stock == null ? null : stock.getLastUpdated());
            stockRows.add(row);
        }

        result.put("hospitalFound", true);
        result.put("hospitalId", hospital.getId());
        result.put("hospitalName", hospital.getName());
        result.put("availableUnits", totalCompatibleUnits);
        result.put("stock", stockRows);
        return result;
    }

    public Map<String, Object> getMultiSourceAvailability(String hospitalName, String receiverBloodGroup, String city) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> hospitalSnapshot = getHospitalStockSnapshot(hospitalName, receiverBloodGroup);
        List<String> compatibleGroups = getCompatibleBloodGroups(receiverBloodGroup);
        List<Map<String, Object>> bloodBanks = new ArrayList<>();
        int bloodBankUnits = 0;

        List<BloodBank> candidateBanks;
        if (city != null && !city.isBlank()) {
            candidateBanks = bloodBankRepository.findByLocationIgnoreCase(city.trim());
        } else {
            candidateBanks = bloodBankRepository.findAll();
        }

        for (BloodBank bank : candidateBanks) {
            String bankGroup = normalizeBloodGroup(bank.getBloodGroup());
            if (!compatibleGroups.contains(bankGroup)) {
                continue;
            }
            bloodBankUnits += bank.getQuantity();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", bank.getName());
            row.put("location", bank.getLocation());
            row.put("bloodGroup", bank.getBloodGroup());
            row.put("quantity", bank.getQuantity());
            row.put("lastUpdated", bank.getLastUpdated());
            bloodBanks.add(row);
        }

        result.put("requestedBloodGroup", normalizeBloodGroup(receiverBloodGroup));
        result.put("compatibleGroups", compatibleGroups);
        result.put("hospitalSource", hospitalSnapshot);
        result.put("bloodBanks", bloodBanks);
        result.put("hospitalUnits", hospitalSnapshot.getOrDefault("availableUnits", 0));
        result.put("bloodBankUnits", bloodBankUnits);
        result.put("totalAvailableUnits",
                ((Number) hospitalSnapshot.getOrDefault("availableUnits", 0)).intValue() + bloodBankUnits);
        return result;
    }

    public Map<String, Object> reserveCompatibleUnits(String hospitalName, String receiverBloodGroup, int requiredUnits) {
        Hospital hospital = findHospitalByName(hospitalName)
                .orElseThrow(() -> new IllegalArgumentException("Hospital stock not found for reservation"));

        List<String> compatibleGroups = getCompatibleBloodGroups(receiverBloodGroup);
        if (compatibleGroups.isEmpty()) {
            throw new IllegalArgumentException("No compatibility mapping found for blood group " + receiverBloodGroup);
        }

        List<BloodStock> selectedStocks = new ArrayList<>();
        int totalAvailable = 0;
        for (String group : compatibleGroups) {
            BloodStock stock = bloodStockRepository.findByHospitalIdAndBloodGroupIgnoreCase(hospital.getId(), group).orElse(null);
            if (stock == null) {
                continue;
            }
            selectedStocks.add(stock);
            totalAvailable += stock.getUnitsAvailable();
        }

        if (totalAvailable < requiredUnits) {
            throw new IllegalArgumentException("Compatible stock unavailable. Required " + requiredUnits + " units, found " + totalAvailable);
        }

        List<Map<String, Object>> reservedFrom = new ArrayList<>();
        int remaining = requiredUnits;
        for (BloodStock stock : selectedStocks) {
            if (remaining <= 0) {
                break;
            }
            int reserved = Math.min(stock.getUnitsAvailable(), remaining);
            if (reserved <= 0) {
                continue;
            }
            stock.setUnitsAvailable(stock.getUnitsAvailable() - reserved);
            stock.setLastUpdated(LocalDateTime.now());
            bloodStockRepository.save(stock);
            remaining -= reserved;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("bloodGroup", stock.getBloodGroup());
            row.put("reservedUnits", reserved);
            row.put("remainingUnits", stock.getUnitsAvailable());
            reservedFrom.add(row);
        }

        Map<String, Object> reservation = new HashMap<>();
        reservation.put("hospitalId", hospital.getId());
        reservation.put("hospitalName", hospital.getName());
        reservation.put("receiverBloodGroup", normalizeBloodGroup(receiverBloodGroup));
        reservation.put("requiredUnits", requiredUnits);
        reservation.put("reservedFrom", reservedFrom);
        reservation.put("compatibleGroups", compatibleGroups);
        reservation.put("remainingCompatibleUnits", totalAvailable - requiredUnits);
        return reservation;
    }

    private Optional<Hospital> findHospitalByName(String hospitalName) {
        if (hospitalName == null || hospitalName.isBlank()) {
            return Optional.empty();
        }
        return hospitalRepository.findFirstByNameIgnoreCase(hospitalName.trim());
    }

    private String normalizeBloodGroup(String bloodGroup) {
        return bloodGroup == null ? "" : bloodGroup.trim().toUpperCase();
    }

    private static Map<String, List<String>> createCompatibilityMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("A+", List.of("A+", "A-", "O+", "O-"));
        map.put("A-", List.of("A-", "O-"));
        map.put("B+", List.of("B+", "B-", "O+", "O-"));
        map.put("B-", List.of("B-", "O-"));
        map.put("AB+", List.of("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"));
        map.put("AB-", List.of("AB-", "A-", "B-", "O-"));
        map.put("O+", List.of("O+", "O-"));
        map.put("O-", List.of("O-"));
        return map;
    }
}
