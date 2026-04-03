package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.Donor;
import com.bloodcare.bloodcare.repository.DonorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DonorSearchService {
    private final DonorRepository donorRepository;

    public DonorSearchService(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    public List<Donor> search(String bloodGroup, String city, Boolean available) {
        boolean availability = available == null || available;
        List<Donor> donors;

        if (!availability) {
            donors = donorRepository.findAll();
        } else if (hasText(bloodGroup) && hasText(city)) {
            donors = donorRepository.findByBloodGroupIgnoreCaseAndCityIgnoreCaseAndAvailableTrue(
                    bloodGroup.trim(), city.trim());
        } else if (hasText(bloodGroup)) {
            donors = donorRepository.findByBloodGroupIgnoreCaseAndAvailableTrue(bloodGroup.trim());
        } else if (hasText(city)) {
            donors = donorRepository.findByCityIgnoreCaseAndAvailableTrue(city.trim());
        } else {
            donors = donorRepository.findByAvailableTrue();
        }

        return donors;
    }

    public List<Map<String, Object>> findNearestDonors(String bloodGroup, String city,
                                                       double latitude, double longitude, int limit) {
        List<Donor> base = search(bloodGroup, city, true);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Donor donor : base) {
            if (donor.getLatitude() == null || donor.getLongitude() == null) {
                continue;
            }
            double distance = haversine(latitude, longitude, donor.getLatitude(), donor.getLongitude());
            Map<String, Object> row = new HashMap<>();
            row.put("donorId", donor.getId());
            row.put("name", donor.getUser() != null ? donor.getUser().getName() : "Donor");
            row.put("bloodGroup", donor.getBloodGroup());
            row.put("city", donor.getCity());
            row.put("available", donor.isAvailable());
            row.put("distanceKm", Math.round(distance * 10.0) / 10.0);
            results.add(row);
        }

        results.sort(Comparator.comparingDouble(row -> (double) row.get("distanceKm")));
        if (limit > 0 && results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int radius = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radius * c;
    }
}
