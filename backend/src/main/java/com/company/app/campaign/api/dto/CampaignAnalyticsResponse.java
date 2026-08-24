package com.company.app.campaign.api.dto;

import java.util.List;
import java.util.UUID;

public record CampaignAnalyticsResponse(
        UUID campaignId,
        AnalyticsSummary summary,
        List<DailyAnalyticsMetrics> dailyBreakdown
) {}
