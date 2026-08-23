package com.company.app.hello.api.dto;

import java.time.Instant;

public record HelloResponse(
        String message,
        String status,
        Instant timestamp
) {
}
