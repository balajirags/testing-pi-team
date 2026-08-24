package com.company.app.hex.service;

import com.company.app.hex.api.dto.HexGenerateRequest;
import com.company.app.hex.api.dto.HexGenerateResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class HexService {

    private final SecureRandom secureRandom = new SecureRandom();

    public HexGenerateResponse generateHex(HexGenerateRequest request) {
        if (request == null || request.text() == null) {
            throw new IllegalArgumentException("text must not be null");
        }

        int length = request.getEffectiveLength();

        if (length < 8 || length > 128) {
            throw new IllegalArgumentException("length must be between 8 and 128");
        }

        if (length % 2 != 0) {
            throw new IllegalArgumentException("length must be an even integer");
        }

        byte[] randomBytes = new byte[length / 2];
        secureRandom.nextBytes(randomBytes);

        StringBuilder sb = new StringBuilder(length);
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }

        return new HexGenerateResponse(request.text(), sb.toString(), length, Instant.now());
    }
}
