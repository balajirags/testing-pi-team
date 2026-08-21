package com.company.app.campaign.service;

import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.UpdateCampaignRequest;
import com.company.app.campaign.domain.AdAccountEntity;
import com.company.app.campaign.domain.BrandEntity;
import com.company.app.campaign.domain.CampaignEntity;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.InvalidStateTransitionException;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.repository.AdAccountRepository;
import com.company.app.campaign.repository.BrandRepository;
import com.company.app.campaign.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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

    @Nested
    @DisplayName("List Campaigns Tests")
    class ListCampaignsTests {

        @Test
        @DisplayName("Should return paginated list of campaigns")
        void listCampaigns_Success() {
            UUID campaignId = UUID.randomUUID();
            Instant now = Instant.now();
            CampaignEntity campaign = CampaignEntity.builder()
                    .id(campaignId)
                    .brandId(brandId)
                    .adAccountId(adAccountId)
                    .name("Q1 Retargeting")
                    .budget(new BigDecimal("5000.00"))
                    .currency("USD")
                    .channel("META")
                    .externalCampaignId("meta_123")
                    .status(CampaignStatus.DRAFT)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Page<CampaignEntity> entityPage = new PageImpl<>(List.of(campaign));
            when(campaignRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

            Page<CampaignResponse> result = campaignService.listCampaigns(brandId, "META", PageRequest.of(0, 20));

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).id()).isEqualTo(campaignId);
            assertThat(result.getContent().get(0).channel()).isEqualTo("META");
        }
    }

    @Nested
    @DisplayName("Get Campaign By ID Tests")
    class GetCampaignByIdTests {

        @Test
        @DisplayName("Should return campaign when campaign exists")
        void getCampaignById_Success() {
            UUID campaignId = UUID.randomUUID();
            Instant now = Instant.now();
            CampaignEntity campaign = CampaignEntity.builder()
                    .id(campaignId)
                    .brandId(brandId)
                    .adAccountId(adAccountId)
                    .name("Q1 Retargeting")
                    .budget(new BigDecimal("5000.00"))
                    .currency("USD")
                    .channel("META")
                    .externalCampaignId("meta_123")
                    .status(CampaignStatus.DRAFT)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            CampaignResponse response = campaignService.getCampaignById(campaignId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(campaignId);
            assertThat(response.name()).isEqualTo("Q1 Retargeting");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when campaign does not exist")
        void getCampaignById_NotFound() {
            UUID campaignId = UUID.randomUUID();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.getCampaignById(campaignId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Campaign not found with ID: " + campaignId);
        }
    }

    @Nested
    @DisplayName("Update Campaign Tests")
    class UpdateCampaignTests {

        @Test
        @DisplayName("Should successfully update campaign when request is valid")
        void updateCampaign_Success() {
            UUID campaignId = UUID.randomUUID();
            Instant now = Instant.now();
            Instant startDate = now.plusSeconds(3600);
            Instant endDate = now.plusSeconds(86400);

            CampaignEntity existing = CampaignEntity.builder()
                    .id(campaignId)
                    .brandId(brandId)
                    .adAccountId(adAccountId)
                    .name("Old Name")
                    .budget(new BigDecimal("1000.00"))
                    .currency("USD")
                    .channel("GOOGLE")
                    .externalCampaignId("goog_1")
                    .status(CampaignStatus.DRAFT)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateCampaignRequest request = new UpdateCampaignRequest(
                    "New Name",
                    new BigDecimal("10000.00"),
                    "EUR",
                    CampaignStatus.ACTIVE,
                    startDate,
                    endDate
            );

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(existing));
            when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            CampaignResponse response = campaignService.updateCampaign(campaignId, request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.budget()).isEqualTo(new BigDecimal("10000.00"));
            assertThat(response.currency()).isEqualTo("EUR");
            assertThat(response.status()).isEqualTo(CampaignStatus.ACTIVE);
            assertThat(response.startDate()).isEqualTo(startDate);
            assertThat(response.endDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when updating non-existent campaign")
        void updateCampaign_NotFound() {
            UUID campaignId = UUID.randomUUID();
            UpdateCampaignRequest request = new UpdateCampaignRequest(
                    "New Name",
                    new BigDecimal("1000.00"),
                    "USD",
                    CampaignStatus.ACTIVE,
                    null,
                    null
            );

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.updateCampaign(campaignId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Campaign not found with ID: " + campaignId);
        }

        @Test
        @DisplayName("Should throw InvalidStateTransitionException when transitioning COMPLETED campaign to DRAFT")
        void updateCampaign_InvalidStatusTransition() {
            UUID campaignId = UUID.randomUUID();
            Instant now = Instant.now();

            CampaignEntity completedCampaign = CampaignEntity.builder()
                    .id(campaignId)
                    .brandId(brandId)
                    .adAccountId(adAccountId)
                    .name("Finished Promo")
                    .budget(new BigDecimal("5000.00"))
                    .currency("USD")
                    .channel("GOOGLE")
                    .externalCampaignId("goog_99")
                    .status(CampaignStatus.COMPLETED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateCampaignRequest request = new UpdateCampaignRequest(
                    null,
                    null,
                    null,
                    CampaignStatus.DRAFT,
                    null,
                    null
            );

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(completedCampaign));

            assertThatThrownBy(() -> campaignService.updateCampaign(campaignId, request))
                    .isInstanceOf(InvalidStateTransitionException.class)
                    .hasMessageContaining("Cannot transition campaign status from COMPLETED to DRAFT");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when end date is before start date")
        void updateCampaign_InvalidDates() {
            UUID campaignId = UUID.randomUUID();
            Instant now = Instant.now();
            Instant startDate = now.plusSeconds(86400);
            Instant endDate = now; // earlier than start date

            CampaignEntity campaign = CampaignEntity.builder()
                    .id(campaignId)
                    .brandId(brandId)
                    .adAccountId(adAccountId)
                    .name("Promo")
                    .budget(new BigDecimal("1000.00"))
                    .currency("USD")
                    .channel("GOOGLE")
                    .externalCampaignId("goog_1")
                    .status(CampaignStatus.DRAFT)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            UpdateCampaignRequest request = new UpdateCampaignRequest(
                    null,
                    null,
                    null,
                    null,
                    startDate,
                    endDate
            );

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            assertThatThrownBy(() -> campaignService.updateCampaign(campaignId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("End date must be equal to or after start date");
        }
    }
}
