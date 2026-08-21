package com.company.app.campaign.api.dto;

import com.company.app.campaign.domain.CampaignStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record UpdateCampaignRequest(
        @Size(min = 1, max = 255, message = "name must not be empty")
        String name,

        @DecimalMin(value = "0.00", message = "budget must be non-negative")
        BigDecimal budget,

        String currency,

        CampaignStatus status,

        Instant startDate,

        Instant endDate
) {
}
