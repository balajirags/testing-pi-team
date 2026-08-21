package com.company.app.campaign.api.dto;

public record CsvRowError(
        int rowNumber,
        String message
) {
}
