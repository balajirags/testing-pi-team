package com.company.app.campaign.api.dto;

import java.util.List;

public record CsvImportSummaryResponse(
        int total,
        int created,
        int failed,
        List<CsvRowError> errors
) {
}
