package com.company.app.campaign.api.dto;

import com.company.app.campaign.domain.CampaignStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        UUID brandId,
        UUID adAccountId,
        String name,
        BigDecimal budget,
        String currency,
        String channel,
        String externalCampaignId,
        CampaignStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
