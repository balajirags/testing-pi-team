package com.company.app.hex.api.dto;

import jakarta.validation.constraints.NotNull;

public record HexGenerateRequest(
        @NotNull(message = "text must not be null")
        String text,

        Integer length
) {
    public int getEffectiveLength() {
        return length != null ? length : 32;
    }
}
