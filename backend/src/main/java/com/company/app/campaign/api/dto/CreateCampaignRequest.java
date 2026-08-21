package com.company.app.campaign.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCampaignRequest(
        @NotNull(message = "brandId is required")
        UUID brandId,

        @NotNull(message = "adAccountId is required")
        UUID adAccountId,

        @NotBlank(message = "name is required")
        String name,

        BigDecimal budget,

        String currency,

        @NotBlank(message = "channel is required")
        String channel,

        @NotBlank(message = "externalCampaignId is required")
        String externalCampaignId
) {
}
