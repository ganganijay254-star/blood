package com.bloodcare.bloodcare.service;

import com.bloodcare.bloodcare.entity.BloodRequest;
import com.bloodcare.bloodcare.entity.BloodStock;
import com.bloodcare.bloodcare.entity.User;
import com.bloodcare.bloodcare.repository.BloodRequestRepository;
import com.bloodcare.bloodcare.repository.BloodRequestResponseRepository;
import com.bloodcare.bloodcare.repository.BloodStockRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BloodRequestService {
    private static final long BLOOD_BANK_FALLBACK_DELAY_SECONDS = 15;

    private final BloodRequestRepository requestRepository;
    private final BloodRequestResponseRepository responseRepository;
    private final BloodStockRepository bloodStockRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public BloodRequestService(BloodRequestRepository requestRepository,
                               BloodRequestResponseRepository responseRepository,
                               BloodStockRepository bloodStockRepository,
                               EmailService emailService,
                               NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.responseRepository = responseRepository;
        this.bloodStockRepository = bloodStockRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    public BloodRequest processRequest(Long requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        String previousStatus = request.getStatus();

        boolean donorAccepted = responseRepository.existsByBloodRequestIdAndStatus(requestId, "ACCEPTED");

        if (donorAccepted || request.getConfirmedDonor() != null) {
            request.setStatus("DONOR_ASSIGNED");
        } else {
            if (!isFallbackWindowReached(request)) {
                request.setStatus("OPEN");
                return requestRepository.save(request);
            }

            List<BloodStock> stock = bloodStockRepository
                    .findByBloodGroupIgnoreCaseAndUnitsAvailableGreaterThan(request.getBloodGroup(), 0);

            if (!stock.isEmpty()) {
                BloodStock selectedStock = selectBestStock(stock, request.getUnitsRequired());
                reserveStockForRequest(selectedStock, request.getUnitsRequired());
                request.setStatus("BLOOD_BANK_AVAILABLE");
                request.setFulfilledDate(LocalDateTime.now());
                if (request.getExpectedDeliveryTime() == null || request.getExpectedDeliveryTime().isBlank()) {
                    request.setExpectedDeliveryTime("Blood is being dispatched from blood bank");
                }
                if (request.getDeliveryLocation() == null || request.getDeliveryLocation().isBlank()) {
                    String fallbackLocation = selectedStock.getBloodBank() != null
                            ? selectedStock.getBloodBank().getLocation()
                            : (selectedStock.getHospital() != null ? selectedStock.getHospital().getName() : "Blood Bank Counter");
                    request.setDeliveryLocation(fallbackLocation);
                }
                notifyBloodBankAvailability(request, selectedStock, previousStatus);
            } else {
                request.setStatus("FAILED");
            }
        }

        return requestRepository.save(request);
    }

    private boolean isFallbackWindowReached(BloodRequest request) {
        LocalDateTime referenceTime = request.getApprovedDate() != null
                ? request.getApprovedDate()
                : request.getCreatedDate();
        if (referenceTime == null) {
            return true;
        }

        long elapsedSeconds = Duration.between(referenceTime, LocalDateTime.now()).getSeconds();
        return elapsedSeconds >= BLOOD_BANK_FALLBACK_DELAY_SECONDS;
    }

    private void notifyBloodBankAvailability(BloodRequest request, BloodStock stock, String previousStatus) {
        if ("BLOOD_BANK_AVAILABLE".equalsIgnoreCase(previousStatus)) {
            return;
        }

        User receiver = request.getUser();
        if (receiver == null) {
            return;
        }

        String hospitalName = stock.getHospital() != null ? stock.getHospital().getName() : request.getHospital();
        String bankName = stock.getBloodBank() != null ? stock.getBloodBank().getName() : "Blood Bank";
        String location = stock.getBloodBank() != null ? stock.getBloodBank().getLocation() : hospitalName;

        notificationService.createNotification(
                receiver,
                "Blood Being Sent",
                "Donor unavailable. Blood is being arranged from " + bankName + " at " + location + " and will be sent shortly.",
                "BLOOD_DELIVERY");

        if (receiver.getEmail() != null && !receiver.getEmail().isBlank()) {
            try {
                emailService.sendBloodBankAvailableEmail(receiver.getEmail(), receiver.getName(), request, stock);
            } catch (Exception mailError) {
                System.out.println("Blood bank availability email failed: " + mailError.getMessage());
            }
        }
    }

    private BloodStock selectBestStock(List<BloodStock> stocks, int requiredUnits) {
        return stocks.stream()
                .sorted((a, b) -> Integer.compare(b.getUnitsAvailable(), a.getUnitsAvailable()))
                .filter(stock -> stock.getUnitsAvailable() >= requiredUnits)
                .findFirst()
                .orElse(stocks.stream()
                        .sorted((a, b) -> Integer.compare(b.getUnitsAvailable(), a.getUnitsAvailable()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No stock found")));
    }

    private void reserveStockForRequest(BloodStock stock, int requiredUnits) {
        int reservedUnits = Math.min(Math.max(requiredUnits, 0), stock.getUnitsAvailable());
        stock.setUnitsAvailable(Math.max(0, stock.getUnitsAvailable() - reservedUnits));
        stock.setLastUpdated(LocalDateTime.now());
        bloodStockRepository.save(stock);
    }
}
