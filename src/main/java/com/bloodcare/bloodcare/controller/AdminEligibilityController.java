package com.bloodcare.bloodcare.controller;

import com.bloodcare.bloodcare.service.SmartEligibilityCheckerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api")
public class AdminEligibilityController {

    @Autowired
    private SmartEligibilityCheckerService checkerService;

    @PostMapping("/match-donors")
    public List<Map<String, Object>> matchDonors(@RequestParam String bloodGroup,
                                                 @RequestParam(required = false) String city,
                                                 @RequestParam(defaultValue = "1") int units,
                                                 @RequestParam(defaultValue = "20") int limit) {
        return checkerService.matchAndNotify(bloodGroup, city, units, limit);
    }
}
