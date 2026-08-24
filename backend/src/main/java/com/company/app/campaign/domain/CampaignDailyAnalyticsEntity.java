package com.company.app.campaign.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "campaign_daily_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignDailyAnalyticsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "impressions", nullable = false)
    private long impressions;

    @Column(name = "clicks", nullable = false)
    private long clicks;

    @Column(name = "spend", nullable = false, precision = 15, scale = 2)
    private BigDecimal spend;

    @Column(name = "conversions", nullable = false)
    private long conversions;
}
