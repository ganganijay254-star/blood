package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.BloodBank;
import com.bloodcare.bloodcare.repository.BloodBankRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BloodBankService {
    private final BloodBankRepository bloodBankRepository;

    public BloodBankService(BloodBankRepository bloodBankRepository) {
        this.bloodBankRepository = bloodBankRepository;
    }

    public BloodBank upsert(String name, String location, String bloodGroup, int quantity) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedLocation = location == null ? "" : location.trim();
        String normalizedGroup = bloodGroup == null ? "" : bloodGroup.trim().toUpperCase();

        BloodBank bank = bloodBankRepository.findByLocationIgnoreCaseAndBloodGroupIgnoreCase(normalizedLocation, normalizedGroup)
                .stream()
                .filter(row -> normalizedName.equalsIgnoreCase(row.getName()))
                .findFirst()
                .orElseGet(BloodBank::new);

        bank.setName(normalizedName);
        bank.setLocation(normalizedLocation);
        bank.setBloodGroup(normalizedGroup);
        bank.setQuantity(Math.max(0, quantity));
        bank.setLastUpdated(LocalDateTime.now());
        return bloodBankRepository.save(bank);
    }

    public List<BloodBank> findAll() {
        return bloodBankRepository.findAll();
    }
}
