package com.company.app.campaign.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyAnalyticsMetrics(
        LocalDate date,
        long impressions,
        long clicks,
        BigDecimal spend,
        long conversions,
        double ctr,
        double cpc
) {}
