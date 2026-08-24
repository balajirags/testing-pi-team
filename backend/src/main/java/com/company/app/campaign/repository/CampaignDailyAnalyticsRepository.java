package com.company.app.campaign.repository;

import com.company.app.campaign.domain.CampaignDailyAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignDailyAnalyticsRepository extends JpaRepository<CampaignDailyAnalyticsEntity, UUID> {
    List<CampaignDailyAnalyticsEntity> findByCampaignIdAndDateBetweenOrderByDateAsc(UUID campaignId, LocalDate startDate, LocalDate endDate);
}
