package com.company.app.campaign.api.dto;

import java.math.BigDecimal;

public record AnalyticsSummary(
        long totalImpressions,
        long totalClicks,
        BigDecimal totalSpend,
        long totalConversions,
        double ctr,
        double cpc
) {}
