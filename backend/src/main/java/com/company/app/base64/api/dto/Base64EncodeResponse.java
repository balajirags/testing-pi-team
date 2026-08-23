package com.company.app.base64.api.dto;

public record Base64EncodeResponse(
        String originalText,
        String encodedText
) {}
