package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.entity.Donor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendResetLink(String toEmail, String link) {
        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        applyFrom(message);
        message.setSubject("BloodCare - Reset Password");
        message.setText(
                "Hello,\n\n"
                + "Click the link below to reset your password:\n\n"
                + link + "\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Regards,\nBloodCare Team"
        );
        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String email, String resetLink) {
        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - Password Reset Request");
        message.setText(
                "Dear User,\n\n"
                + "We received a request to reset your password.\n\n"
                + "Click the link below to reset it:\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "BloodCare Support Team"
        );
        mailSender.send(message);
    }

    public void sendVisitApprovalEmail(String email, String name, String hospital) {
        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - Donation Visit Approved");
        message.setText(
                "Dear " + name + ",\n\n"
                + "Your blood donation visit request has been APPROVED.\n\n"
                + "Hospital: " + hospital + "\n\n"
                + "Please visit on your scheduled time.\n\n"
                + "Thank you for saving lives.\n\n"
                + "BloodCare Team"
        );
        mailSender.send(message);
    }

    public void sendVisitRejectionEmail(String email, String name, String hospital) {
        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - Donation Visit Update");
        message.setText(
                "Dear " + name + ",\n\n"
                + "We regret to inform you that your blood donation visit request\n"
                + "for hospital: " + hospital + " has been REJECTED.\n\n"
                + "You may try again later or contact support.\n\n"
                + "Thank you.\n"
                + "BloodCare Team"
        );
        mailSender.send(message);
    }

    public void sendEligibleDonorEmail(String email, String name, String bloodGroup, String city) {
        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - New Blood Request Matched");
        message.setText(
                "Dear " + safe(name, "Donor") + ",\n\n"
                + "A nearby blood request matches your donor profile.\n\n"
                + "Blood Group: " + safe(bloodGroup, "-") + "\n"
                + "City: " + safe(city, "-") + "\n\n"
                + "Please login to BloodCare donor dashboard and respond to the request.\n\n"
                + "Thank you for helping save lives.\n\n"
                + "BloodCare Team"
        );
        mailSender.send(message);
    }

    public void sendDonorAcceptedEmail(String email, String receiverName, BloodRequest request, Donor donor) {
        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - Donor Accepted Your Request (" + safe(request != null ? request.getPublicId() : null, "Request") + ")");

        String donorName = (donor != null && donor.getUser() != null) ? safe(donor.getUser().getName(), "Donor") : "Donor";
        String donorPhone = (donor != null && donor.getUser() != null) ? safe(donor.getUser().getMobile(), "Not provided") : "Not provided";
        String donorEmail = (donor != null && donor.getUser() != null) ? safe(donor.getUser().getEmail(), "Not provided") : "Not provided";
        String acceptedAt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(LocalDateTime.now());

        String body =
                "Dear " + safe(receiverName, "User") + ",\n\n"
                + "Good news. A donor has accepted your blood request.\n\n"
                + "Request Details:\n"
                + "- Request ID: " + safe(request != null ? request.getPublicId() : null, "-") + "\n"
                + "- Patient Name: " + safe(request != null ? request.getPatientName() : null, "-") + "\n"
                + "- Blood Group: " + safe(request != null ? request.getBloodGroup() : null, "-") + "\n"
                + "- Units Required: " + (request != null ? request.getUnitsRequired() : 0) + "\n"
                + "- Urgency: " + safe(request != null ? request.getUrgency() : null, "-") + "\n"
                + "- Hospital: " + safe(request != null ? request.getHospital() : null, "-") + "\n"
                + "- City: " + safe(request != null ? request.getCity() : null, "-") + "\n"
                + "- Contact Number (Request): " + safe(request != null ? request.getContactNumber() : null, "-") + "\n\n"
                + "Donor Details:\n"
                + "- Donor Name: " + donorName + "\n"
                + "- Donor Blood Group: " + safe(donor != null ? donor.getBloodGroup() : null, "-") + "\n"
                + "- Donor Contact: " + donorPhone + "\n"
                + "- Donor Email: " + donorEmail + "\n"
                + "- Accepted At: " + acceptedAt + "\n\n"
                + "Please coordinate with the donor and hospital as soon as possible.\n\n"
                + "Regards,\n"
                + "BloodCare Team";

        message.setText(body);
        mailSender.send(message);
    }

    public void sendBloodRequestApprovalEmail(String email, String receiverName, BloodRequest request) {
        if (email == null || email.isBlank() || request == null) {
            return;
        }

        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - Blood Request Approved (" + safe(request.getPublicId(), "Request") + ")");

        String status = safe(request.getStatus(), "OPEN").toUpperCase();
        String deliveryMode = "RESERVED".equals(status)
                ? "Blood units have been reserved and prepared for delivery."
                : "Your request is approved and is now being coordinated through donor matching.";
        String instructions = safe(request.getDeliveryInstructions(), "Please keep your phone reachable and carry any required hospital documents.");
        String approvedAt = request.getApprovedDate() == null
                ? "Just now"
                : DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").format(request.getApprovedDate());

        String body =
                "Dear " + safe(receiverName, "User") + ",\n\n"
                + "Your blood request has been approved successfully.\n\n"
                + "Request Details:\n"
                + "- Request ID: " + safe(request.getPublicId(), "-") + "\n"
                + "- Patient Name: " + safe(request.getPatientName(), "-") + "\n"
                + "- Blood Group: " + safe(request.getBloodGroup(), "-") + "\n"
                + "- Units Required: " + request.getUnitsRequired() + "\n"
                + "- Hospital: " + safe(request.getHospital(), "-") + "\n"
                + "- City: " + safe(request.getCity(), "-") + "\n"
                + "- Approval Time: " + approvedAt + "\n"
                + "- Status: " + status + "\n\n"
                + "Delivery Update:\n"
                + "- Expected Time: " + safe(request.getExpectedDeliveryTime(), "Within 2-3 hours") + "\n"
                + "- Delivery Location: " + safe(request.getDeliveryLocation(), safe(request.getHospital(), "Hospital Counter")) + "\n"
                + "- Process: " + deliveryMode + "\n"
                + "- Instructions: " + instructions + "\n\n"
                + "What happens next:\n"
                + "- Our team will coordinate the blood issue/delivery.\n"
                + "- Please remain available on your registered contact number.\n"
                + "- If hospital staff asks, share your request ID: " + safe(request.getPublicId(), "-") + "\n\n"
                + "Regards,\n"
                + "BloodCare Team";

        message.setText(body);
        mailSender.send(message);
    }

    public void sendBloodBankAvailableEmail(String email, String receiverName, BloodRequest request, BloodStock stock) {
        if (email == null || email.isBlank() || request == null || stock == null) {
            return;
        }

        ensureEmailConfigured();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        applyFrom(message);
        message.setSubject("BloodCare - Blood Dispatched From Blood Bank (" + safe(request.getPublicId(), "Request") + ")");

        String hospitalName = stock.getHospital() != null ? safe(stock.getHospital().getName(), "-") : "-";
        String bankName = stock.getBloodBank() != null ? safe(stock.getBloodBank().getName(), "-") : "Blood Bank";
        String bankLocation = stock.getBloodBank() != null ? safe(stock.getBloodBank().getLocation(), "-") : hospitalName;

        String body =
                "Dear " + safe(receiverName, "User") + ",\n\n"
                + "Donor is currently unavailable, but matching blood has been arranged from our blood bank network and is being sent to you.\n\n"
                + "Request Details:\n"
                + "- Request ID: " + safe(request.getPublicId(), "-") + "\n"
                + "- Blood Group: " + safe(request.getBloodGroup(), "-") + "\n"
                + "- Units Required: " + request.getUnitsRequired() + "\n"
                + "- Current Status: BLOOD_BANK_AVAILABLE\n\n"
                + "Available Source:\n"
                + "- Hospital: " + hospitalName + "\n"
                + "- Blood Bank: " + bankName + "\n"
                + "- Location: " + bankLocation + "\n"
                + "- Available Units: " + stock.getUnitsAvailable() + "\n\n"
                + "Dispatch Update:\n"
                + "- Blood is being issued from the listed source.\n"
                + "- You will continue with the same process flow.\n"
                + "Please keep your phone reachable for the next update.\n\n"
                + "Regards,\n"
                + "BloodCare Team";

        message.setText(body);
        mailSender.send(message);
    }

    public boolean isEmailConfigured() {
        return mailUsername != null && !mailUsername.isBlank();
    }

    private void ensureEmailConfigured() {
        if (!isEmailConfigured()) {
            throw new IllegalStateException("Email is not configured. Set MAIL_USERNAME and MAIL_PASSWORD before sending mail.");
        }
    }

    private void applyFrom(SimpleMailMessage message) {
        if (isEmailConfigured()) {
            message.setFrom(mailUsername);
        }
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value;
    }
}
