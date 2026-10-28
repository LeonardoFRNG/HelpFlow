package com.helpflow.dispatch.service;

import com.helpflow.dispatch.dto.DispatchRequest;
import com.helpflow.dispatch.dto.DispatchResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class TechnicianDispatchService {

    private static final int MAX_TECHNICIANS = 20;

    public DispatchResponse dispatch(DispatchRequest request) {
        String normalizedCategory = normalize(request.category());
        String normalizedPriority = normalize(request.priority());

        long seed = calculateSeed(request.ticketId(), normalizedCategory, normalizedPriority);


        int techIndex = (int) (Math.floorMod(seed, MAX_TECHNICIANS) + 1);
        String technicianCode = String.format("TEC-%03d", techIndex);


        int estimatedHours = calculateEstimatedHours(normalizedPriority, seed);


        String availabilityEn;
        String availabilityEs;
        if (estimatedHours <= 2) {
            availabilityEn = "HIGH";
            availabilityEs = "ALTA";
        } else if (estimatedHours <= 8) {
            availabilityEn = "MEDIUM";
            availabilityEs = "MEDIA";
        } else {
            availabilityEn = "LOW";
            availabilityEs = "BAJA";
        }

        return DispatchResponse.of(request.ticketId(), technicianCode, availabilityEn, availabilityEs, estimatedHours);
    }

    private int calculateEstimatedHours(String priority, long seed) {
        return switch (priority) {
            case "CRITICAL", "CRITICA", "CRÍTICA", "CRITICO", "CRÍTICO" ->
                    (int) (Math.floorMod(seed >> 2, 2) + 1); // 1 or 2 hours
            case "HIGH", "ALTA", "ALTO" ->
                    (int) (Math.floorMod(seed >> 2, 4) + 2); // 2 to 5 hours
            case "MEDIUM", "MEDIA", "MEDIO" ->
                    (int) (Math.floorMod(seed >> 2, 6) + 3); // 3 to 8 hours
            default ->
                    (int) (Math.floorMod(seed >> 2, 8) + 5); // 5 to 12 hours
        };
    }

    private long calculateSeed(Long ticketId, String category, String priority) {
        try {
            String combined = ticketId + ":" + category + ":" + priority;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hashBytes[i] & 0xFF);
            }
            return Math.abs(seed);
        } catch (NoSuchAlgorithmException e) {
            return Math.abs((ticketId + ":" + category + ":" + priority).hashCode());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase();
    }
}
