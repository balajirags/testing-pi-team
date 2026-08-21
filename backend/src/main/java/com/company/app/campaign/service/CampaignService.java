package com.company.app.campaign.service;

import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CsvImportSummaryResponse;
import com.company.app.campaign.api.dto.CsvRowError;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Transactional
    public CsvImportSummaryResponse importCampaignsFromCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file must not be empty");
        }

        List<CsvRowError> errors = new ArrayList<>();
        int created = 0;
        int total = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file must contain a header row");
            }

            List<String> headers = parseCsvLine(headerLine);
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                headerMap.put(headers.get(i).trim().toLowerCase(), i);
            }

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }

            if (lines.size() > 1000) {
                throw new IllegalArgumentException("CSV import exceeds maximum limit of 1000 rows");
            }

            int rowNum = 1; // Header is row 1
            for (String rowLine : lines) {
                rowNum++;
                total++;
                List<String> values = parseCsvLine(rowLine);
                try {
                    processCsvRow(values, headerMap);
                    created++;
                } catch (Exception ex) {
                    errors.add(new CsvRowError(rowNum, ex.getMessage()));
                }
            }

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse CSV file: " + ex.getMessage(), ex);
        }

        int failed = total - created;
        return new CsvImportSummaryResponse(total, created, failed, errors);
    }

    private void processCsvRow(List<String> values, Map<String, Integer> headerMap) {
        String name = getCsvValue(values, headerMap, "name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        String brandIdStr = getCsvValue(values, headerMap, "brand_id");
        if (brandIdStr == null || brandIdStr.isBlank()) {
            brandIdStr = getCsvValue(values, headerMap, "brandid");
        }
        if (brandIdStr == null || brandIdStr.isBlank()) {
            throw new IllegalArgumentException("brand_id is required");
        }
        UUID brandId;
        try {
            brandId = UUID.fromString(brandIdStr);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid brand_id UUID format: " + brandIdStr);
        }
        if (!brandRepository.existsById(brandId)) {
            throw new ResourceNotFoundException("Brand not found with ID: " + brandId);
        }

        String adAccountIdStr = getCsvValue(values, headerMap, "ad_account_id");
        if (adAccountIdStr == null || adAccountIdStr.isBlank()) {
            adAccountIdStr = getCsvValue(values, headerMap, "adaccountid");
        }
        if (adAccountIdStr == null || adAccountIdStr.isBlank()) {
            throw new IllegalArgumentException("ad_account_id is required");
        }
        UUID adAccountId;
        try {
            adAccountId = UUID.fromString(adAccountIdStr);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid ad_account_id UUID format: " + adAccountIdStr);
        }
        if (!adAccountRepository.existsById(adAccountId)) {
            throw new ResourceNotFoundException("Ad account not found with ID: " + adAccountId);
        }

        String channel = getCsvValue(values, headerMap, "channel");
        if (channel == null || channel.isBlank()) {
            throw new IllegalArgumentException("channel is required");
        }

        String externalCampaignId = getCsvValue(values, headerMap, "external_campaign_id");
        if (externalCampaignId == null || externalCampaignId.isBlank()) {
            externalCampaignId = getCsvValue(values, headerMap, "externalcampaignid");
        }
        if (externalCampaignId == null || externalCampaignId.isBlank()) {
            throw new IllegalArgumentException("external_campaign_id is required");
        }

        if (campaignRepository.existsByChannelAndExternalCampaignId(channel, externalCampaignId)) {
            throw new DuplicateResourceException(String.format(
                    "Campaign mapping already exists for channel '%s' and external campaign ID '%s'",
                    channel, externalCampaignId));
        }

        String budgetStr = getCsvValue(values, headerMap, "budget");
        BigDecimal budget = null;
        if (budgetStr != null && !budgetStr.isBlank()) {
            try {
                budget = new BigDecimal(budgetStr);
                if (budget.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("budget must be non-negative");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid budget number format: " + budgetStr);
            }
        }

        String currency = getCsvValue(values, headerMap, "currency");
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }

        CampaignEntity campaign = CampaignEntity.builder()
                .brandId(brandId)
                .adAccountId(adAccountId)
                .name(name)
                .budget(budget)
                .currency(currency)
                .channel(channel)
                .externalCampaignId(externalCampaignId)
                .status(CampaignStatus.DRAFT)
                .build();

        campaignRepository.save(campaign);
    }

    private String getCsvValue(List<String> values, Map<String, Integer> headerMap, String key) {
        Integer idx = headerMap.get(key.toLowerCase());
        if (idx != null && idx < values.size()) {
            return values.get(idx);
        }
        return null;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null || line.isBlank()) {
            return values;
        }
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());
        return values;
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
