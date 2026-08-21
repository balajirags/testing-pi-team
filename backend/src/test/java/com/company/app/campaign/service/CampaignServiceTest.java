package com.company.app.campaign.service;

import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.domain.AdAccountEntity;
import com.company.app.campaign.domain.BrandEntity;
import com.company.app.campaign.domain.CampaignEntity;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.repository.AdAccountRepository;
import com.company.app.campaign.repository.BrandRepository;
import com.company.app.campaign.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private AdAccountRepository adAccountRepository;

    @InjectMocks
    private CampaignService campaignService;

    private UUID brandId;
    private UUID adAccountId;

    @BeforeEach
    void setUp() {
        brandId = UUID.randomUUID();
        adAccountId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully create campaign when request is valid")
    void createCampaign_HappyPath() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                null, // currency omitted -> defaults to USD
                "META",
                "meta_12345"
        );

        when(brandRepository.existsById(brandId)).thenReturn(true);
        when(adAccountRepository.existsById(adAccountId)).thenReturn(true);
        when(campaignRepository.existsByChannelAndExternalCampaignId("META", "meta_12345")).thenReturn(false);

        UUID campaignId = UUID.randomUUID();
        Instant now = Instant.now();
        CampaignEntity savedEntity = CampaignEntity.builder()
                .id(campaignId)
                .brandId(brandId)
                .adAccountId(adAccountId)
                .name("Q1 Retargeting")
                .budget(new BigDecimal("5000.00"))
                .currency("USD")
                .channel("META")
                .externalCampaignId("meta_12345")
                .status(CampaignStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(savedEntity);

        CampaignResponse response = campaignService.createCampaign(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(campaignId);
        assertThat(response.brandId()).isEqualTo(brandId);
        assertThat(response.adAccountId()).isEqualTo(adAccountId);
        assertThat(response.name()).isEqualTo("Q1 Retargeting");
        assertThat(response.budget()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.channel()).isEqualTo("META");
        assertThat(response.externalCampaignId()).isEqualTo("meta_12345");
        assertThat(response.status()).isEqualTo(CampaignStatus.DRAFT);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when brand does not exist")
    void createCampaign_BrandNotFound() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        when(brandRepository.existsById(brandId)).thenReturn(false);

        assertThatThrownBy(() -> campaignService.createCampaign(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Brand not found");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when ad account does not exist")
    void createCampaign_AdAccountNotFound() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        when(brandRepository.existsById(brandId)).thenReturn(true);
        when(adAccountRepository.existsById(adAccountId)).thenReturn(false);

        assertThatThrownBy(() -> campaignService.createCampaign(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Ad account not found");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when channel and external campaign mapping already exists")
    void createCampaign_DuplicateMapping() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        when(brandRepository.existsById(brandId)).thenReturn(true);
        when(adAccountRepository.existsById(adAccountId)).thenReturn(true);
        when(campaignRepository.existsByChannelAndExternalCampaignId("META", "meta_12345")).thenReturn(true);

        assertThatThrownBy(() -> campaignService.createCampaign(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Campaign mapping already exists");
    }
}
