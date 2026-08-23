package com.company.app.base64.api.dto;

import jakarta.validation.constraints.NotNull;

public record Base64EncodeRequest(
        @NotNull(message = "must not be null")
        String text
) {}
