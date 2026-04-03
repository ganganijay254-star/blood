package com.bloodcare.bloodcare.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CertificateViewController {

    @GetMapping("/certificate")
    public String viewCertificate() {
        return "certificate";
    }

    @GetMapping("/verify-certificate")
    public String verifyCertificate() {
        return "verify-certificate";
    }
}
