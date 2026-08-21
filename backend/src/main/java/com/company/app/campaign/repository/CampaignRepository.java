package com.company.app.campaign.repository;

import com.company.app.campaign.domain.CampaignEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<CampaignEntity, UUID> {
    boolean existsByChannelAndExternalCampaignId(String channel, String externalCampaignId);
    Optional<CampaignEntity> findByChannelAndExternalCampaignId(String channel, String externalCampaignId);
}
