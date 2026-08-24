package com.company.app.hex.api.dto;

import java.time.Instant;

public record HexGenerateResponse(
        String text,
        String hex,
        int length,
        Instant timestamp
) {}
