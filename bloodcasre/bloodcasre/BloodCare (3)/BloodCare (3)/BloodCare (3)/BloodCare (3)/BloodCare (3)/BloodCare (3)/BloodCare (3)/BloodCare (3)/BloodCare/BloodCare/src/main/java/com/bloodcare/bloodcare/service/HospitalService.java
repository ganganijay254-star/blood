package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.dto.HospitalDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class HospitalService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<HospitalDTO> findNearby(double userLat, double userLng) {

        String url =
            "https://nominatim.openstreetmap.org/search?format=json&q=hospital"
            + "&limit=20"
            + "&viewbox=" + (userLng-0.2) + "," + (userLat+0.2) + ","
            + (userLng+0.2) + "," + (userLat-0.2)
            + "&bounded=1";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "BloodCareApp");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        List<Map<String,Object>> body = response.getBody();
        if (body == null) return new ArrayList<>();

        return body.stream()
                .map(h -> {

                    String displayName = (String) h.get("display_name");
                    String name = displayName.split(",")[0];

                    double lat = Double.parseDouble((String) h.get("lat"));
                    double lon = Double.parseDouble((String) h.get("lon"));

                    double distance = calculateDistance(
                            userLat, userLng, lat, lon);

                    return new HospitalDTO(
                            null,
                            name,
                            displayName,
                            Math.round(distance * 10.0) / 10.0
                    );
                })

                // ✅ 5 KM FILTER
                .filter(h -> h.getDistance() <= 5)

                // ✅ SORT BY DISTANCE
                .sorted(Comparator.comparingDouble(HospitalDTO::getDistance))

                .limit(10)

                .collect(Collectors.toList());
    }

    private double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        final int R = 6371;

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}