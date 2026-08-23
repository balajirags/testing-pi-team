package com.company.app.hash.service;

import com.company.app.hash.api.dto.RandomHashResponse;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RandomHashService {

    public RandomHashResponse generateRandomHash() {
        String randomInput = UUID.randomUUID().toString() + "-" + System.nanoTime();
        String hash = computeSha256(randomInput, "SHA-256");
        return new RandomHashResponse(hash, "SUCCESS", Instant.now());
    }

    String computeSha256(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] encodedHash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available: " + algorithm, e);
        }
    }
}
