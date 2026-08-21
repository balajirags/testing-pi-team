package com.company.app.campaign.service;

import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.UpdateCampaignRequest;
import com.company.app.campaign.domain.CampaignEntity;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.InvalidStateTransitionException;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.repository.AdAccountRepository;
import com.company.app.campaign.repository.BrandRepository;
import com.company.app.campaign.repository.CampaignRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public Page<CampaignResponse> listCampaigns(UUID brandId, String channel, CampaignStatus status, Pageable pageable) {
        Specification<CampaignEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), CampaignStatus.ARCHIVED));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return campaignRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(UUID id) {
        CampaignEntity campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));
        return mapToResponse(campaign);
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID id, UpdateCampaignRequest request) {
        CampaignEntity campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        if (request.status() != null && request.status() != campaign.getStatus()) {
            validateStatusTransition(campaign.getStatus(), request.status());
            campaign.setStatus(request.status());
        }

        Instant effectiveStartDate = request.startDate() != null ? request.startDate() : campaign.getStartDate();
        Instant effectiveEndDate = request.endDate() != null ? request.endDate() : campaign.getEndDate();

        if (effectiveStartDate != null && effectiveEndDate != null && effectiveEndDate.isBefore(effectiveStartDate)) {
            throw new IllegalArgumentException("End date must be equal to or after start date");
        }

        if (request.name() != null) {
            campaign.setName(request.name());
        }
        if (request.budget() != null) {
            campaign.setBudget(request.budget());
        }
        if (request.currency() != null) {
            campaign.setCurrency(request.currency());
        }
        if (request.startDate() != null) {
            campaign.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            campaign.setEndDate(request.endDate());
        }

        CampaignEntity saved = campaignRepository.save(campaign);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteCampaign(UUID id) {
        CampaignEntity campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with ID: " + id));

        campaign.setStatus(CampaignStatus.ARCHIVED);
        campaignRepository.save(campaign);
    }

    private void validateStatusTransition(CampaignStatus currentStatus, CampaignStatus newStatus) {
        if (currentStatus == CampaignStatus.COMPLETED) {
            if (newStatus != CampaignStatus.COMPLETED && newStatus != CampaignStatus.ARCHIVED) {
                throw new InvalidStateTransitionException(
                        String.format("Cannot transition campaign status from %s to %s", currentStatus, newStatus));
            }
        } else if (currentStatus == CampaignStatus.ARCHIVED) {
            if (newStatus != CampaignStatus.ARCHIVED) {
                throw new InvalidStateTransitionException(
                        String.format("Cannot transition campaign status from %s to %s", currentStatus, newStatus));
            }
        }
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
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
