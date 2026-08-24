package com.company.app.password.api.dto;

import java.time.Instant;

public record PasswordGenerateResponse(
        String password,
        int length,
        Instant timestamp
) {}
