package com.company.app.password.service;

import com.company.app.password.api.dto.PasswordGenerateResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;

@Service
public class PasswordService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordGenerateResponse generatePassword(int length) {
        if (length < 8 || length > 64) {
            throw new IllegalArgumentException("length must be between 8 and 64");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }

        return new PasswordGenerateResponse(sb.toString(), length, Instant.now());
    }
}
