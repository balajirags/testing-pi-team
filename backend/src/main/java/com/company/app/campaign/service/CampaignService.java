package com.company.app.campaign.service;

import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.domain.CampaignEntity;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.repository.AdAccountRepository;
import com.company.app.campaign.repository.BrandRepository;
import com.company.app.campaign.repository.CampaignRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final BrandRepository brandRepository;
    private final AdAccountRepository adAccountRepository;

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        if (!brandRepository.existsById(request.brandId())) {
            throw new ResourceNotFoundException("Brand not found with ID: " + request.brandId());
        }

        if (!adAccountRepository.existsById(request.adAccountId())) {
            throw new ResourceNotFoundException("Ad account not found with ID: " + request.adAccountId());
        }

        if (campaignRepository.existsByChannelAndExternalCampaignId(request.channel(), request.externalCampaignId())) {
            throw new DuplicateResourceException(String.format(
                    "Campaign mapping already exists for channel '%s' and external campaign ID '%s'",
                    request.channel(), request.externalCampaignId()));
        }

        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency()
                : "USD";

        CampaignEntity campaign = CampaignEntity.builder()
                .brandId(request.brandId())
                .adAccountId(request.adAccountId())
                .name(request.name())
                .budget(request.budget())
                .currency(currency)
                .channel(request.channel())
                .externalCampaignId(request.externalCampaignId())
                .status(CampaignStatus.DRAFT)
                .build();

        CampaignEntity saved = campaignRepository.save(campaign);

        return mapToResponse(saved);
    }

    private CampaignResponse mapToResponse(CampaignEntity entity) {
        return new CampaignResponse(
                entity.getId(),
                entity.getBrandId(),
                entity.getAdAccountId(),
                entity.getName(),
                entity.getBudget(),
                entity.getCurrency(),
                entity.getChannel(),
                entity.getExternalCampaignId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
