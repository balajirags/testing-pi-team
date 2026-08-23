package com.company.app.hash.api.dto;

import java.time.Instant;

public record RandomHashResponse(
        String hash,
        String status,
        Instant timestamp
) {
}
