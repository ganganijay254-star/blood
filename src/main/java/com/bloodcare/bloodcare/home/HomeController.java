package com.bloodcare.bloodcare.home;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // ✅ Spring Boot automatically serves index.html from static folder
    // Removed the mapping so index.html loads instead of this message
}
