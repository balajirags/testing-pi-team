package com.company.app.campaign.service;

import com.company.app.campaign.api.dto.CampaignAnalyticsResponse;
import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CsvImportSummaryResponse;
import com.company.app.campaign.api.dto.UpdateCampaignRequest;
import com.company.app.campaign.domain.AdAccountEntity;
import com.company.app.campaign.domain.BrandEntity;
import com.company.app.campaign.domain.CampaignDailyAnalyticsEntity;
import com.company.app.campaign.domain.CampaignEntity;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.InvalidStateTransitionException;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.repository.AdAccountRepository;
import com.company.app.campaign.repository.BrandRepository;
import com.company.app.campaign.repository.CampaignDailyAnalyticsRepository;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

    @Mock
    private CampaignDailyAnalyticsRepository campaignDailyAnalyticsRepository;

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

            Page<CampaignResponse> result = campaignService.listCampaigns(brandId, "META", null, PageRequest.of(0, 20));

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

    @Nested
    @DisplayName("Delete Campaign Tests")
    class DeleteCampaignTests {

        @Test
        @DisplayName("Should soft-delete campaign by setting status to ARCHIVED")
        void deleteCampaign_Success() {
            UUID campaignId = UUID.randomUUID();
            Instant now = Instant.now();

            CampaignEntity campaign = CampaignEntity.builder()
                    .id(campaignId)
                    .brandId(brandId)
                    .adAccountId(adAccountId)
                    .name("Promo")
                    .budget(new BigDecimal("1000.00"))
                    .currency("USD")
                    .channel("GOOGLE")
                    .externalCampaignId("goog_1")
                    .status(CampaignStatus.ACTIVE)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));

            campaignService.deleteCampaign(campaignId);

            assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ARCHIVED);
            verify(campaignRepository).save(campaign);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent campaign")
        void deleteCampaign_NotFound() {
            UUID campaignId = UUID.randomUUID();
            when(campaignRepository.findById(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> campaignService.deleteCampaign(campaignId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Campaign not found with ID: " + campaignId);
        }
    }

    @Nested
    @DisplayName("CSV Import Tests")
    class CsvImportTests {

        @Test
        @DisplayName("Should import valid CSV rows successfully")
        void importCampaignsFromCsv_Success() {
            String csvContent = "name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id\n" +
                    "Campaign 1," + brandId + "," + adAccountId + ",1000.00,USD,META,meta_csv_1\n" +
                    "Campaign 2," + brandId + "," + adAccountId + ",2000.00,USD,GOOGLE,goog_csv_2";

            MockMultipartFile file = new MockMultipartFile("file", "campaigns.csv", "text/csv", csvContent.getBytes());

            when(brandRepository.existsById(brandId)).thenReturn(true);
            when(adAccountRepository.existsById(adAccountId)).thenReturn(true);
            when(campaignRepository.existsByChannelAndExternalCampaignId(any(), any())).thenReturn(false);

            CsvImportSummaryResponse summary = campaignService.importCampaignsFromCsv(file);

            assertThat(summary).isNotNull();
            assertThat(summary.total()).isEqualTo(2);
            assertThat(summary.created()).isEqualTo(2);
            assertThat(summary.failed()).isEqualTo(0);
            assertThat(summary.errors()).isEmpty();
        }

        @Test
        @DisplayName("Should record partial errors for invalid CSV rows")
        void importCampaignsFromCsv_PartialErrors() {
            UUID invalidBrandId = UUID.randomUUID();
            String csvContent = "name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id\n" +
                    "Valid Campaign," + brandId + "," + adAccountId + ",1000.00,USD,META,meta_csv_valid\n" +
                    "Invalid Brand Campaign," + invalidBrandId + "," + adAccountId + ",2000.00,USD,GOOGLE,goog_csv_invalid\n" +
                    "Missing Channel Campaign," + brandId + "," + adAccountId + ",3000.00,USD,,goog_csv_nochannel";

            MockMultipartFile file = new MockMultipartFile("file", "campaigns.csv", "text/csv", csvContent.getBytes());

            when(brandRepository.existsById(brandId)).thenReturn(true);
            when(brandRepository.existsById(invalidBrandId)).thenReturn(false);
            when(adAccountRepository.existsById(adAccountId)).thenReturn(true);
            when(campaignRepository.existsByChannelAndExternalCampaignId("META", "meta_csv_valid")).thenReturn(false);

            CsvImportSummaryResponse summary = campaignService.importCampaignsFromCsv(file);

            assertThat(summary).isNotNull();
            assertThat(summary.total()).isEqualTo(3);
            assertThat(summary.created()).isEqualTo(1);
            assertThat(summary.failed()).isEqualTo(2);
            assertThat(summary.errors()).hasSize(2);
            assertThat(summary.errors().get(0).rowNumber()).isEqualTo(3);
            assertThat(summary.errors().get(0).message()).contains("Brand not found");
            assertThat(summary.errors().get(1).rowNumber()).isEqualTo(4);
            assertThat(summary.errors().get(1).message()).contains("channel is required");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when CSV file is empty")
        void importCampaignsFromCsv_EmptyFile() {
            MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

            assertThatThrownBy(() -> campaignService.importCampaignsFromCsv(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CSV file must not be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when header is missing or blank")
        void importCampaignsFromCsv_MissingHeader() {
            MockMultipartFile file = new MockMultipartFile("file", "blank.csv", "text/csv", "\n\n".getBytes());

            assertThatThrownBy(() -> campaignService.importCampaignsFromCsv(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CSV file must contain a header row");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when CSV row count exceeds 1000")
        void importCampaignsFromCsv_ExceedsRowLimit() {
            StringBuilder sb = new StringBuilder("name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id\n");
            for (int i = 0; i < 1001; i++) {
                sb.append("Camp ").append(i).append(",").append(brandId).append(",").append(adAccountId).append(",100,USD,META,ext_").append(i).append("\n");
            }
            MockMultipartFile file = new MockMultipartFile("file", "large.csv", "text/csv", sb.toString().getBytes());

            assertThatThrownBy(() -> campaignService.importCampaignsFromCsv(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds maximum limit of 1000 rows");
        }

        @Test
        @DisplayName("Should record error for missing mandatory fields, invalid UUIDs, negative budgets, and duplicate mapping")
        void importCampaignsFromCsv_RowValidationErrors() {
            UUID missingAdAccountId = UUID.randomUUID();
            String csvContent = "name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id\n" +
                    "," + brandId + "," + adAccountId + ",100,USD,META,ext_1\n" + // missing name
                    "Row 3,not-a-uuid," + adAccountId + ",100,USD,META,ext_2\n" + // invalid brand_id UUID
                    "Row 4," + brandId + ",not-a-uuid,100,USD,META,ext_3\n" + // invalid ad_account_id UUID
                    "Row 5," + brandId + "," + missingAdAccountId + ",100,USD,META,ext_4\n" + // ad_account not found
                    "Row 6," + brandId + "," + adAccountId + ",100,USD,META,\n" + // missing external_campaign_id
                    "Row 7," + brandId + "," + adAccountId + ",100,USD,META,ext_dup\n" + // duplicate mapping
                    "Row 8," + brandId + "," + adAccountId + ",-100,USD,META,ext_8\n" + // negative budget
                    "Row 9," + brandId + "," + adAccountId + ",not-a-number,USD,META,ext_9\n"; // invalid budget number

            MockMultipartFile file = new MockMultipartFile("file", "validation.csv", "text/csv", csvContent.getBytes());

            when(brandRepository.existsById(brandId)).thenReturn(true);
            when(adAccountRepository.existsById(adAccountId)).thenReturn(true);
            when(adAccountRepository.existsById(missingAdAccountId)).thenReturn(false);
            when(campaignRepository.existsByChannelAndExternalCampaignId("META", "ext_dup")).thenReturn(true);
            when(campaignRepository.existsByChannelAndExternalCampaignId("META", "ext_8")).thenReturn(false);
            when(campaignRepository.existsByChannelAndExternalCampaignId("META", "ext_9")).thenReturn(false);

            CsvImportSummaryResponse summary = campaignService.importCampaignsFromCsv(file);

            assertThat(summary.total()).isEqualTo(8);
            assertThat(summary.created()).isEqualTo(0);
            assertThat(summary.failed()).isEqualTo(8);
            assertThat(summary.errors()).hasSize(8);
            assertThat(summary.errors().get(0).message()).contains("name is required");
            assertThat(summary.errors().get(1).message()).contains("Invalid brand_id UUID format");
            assertThat(summary.errors().get(2).message()).contains("Invalid ad_account_id UUID format");
            assertThat(summary.errors().get(3).message()).contains("Ad account not found");
            assertThat(summary.errors().get(4).message()).contains("external_campaign_id is required");
            assertThat(summary.errors().get(5).message()).contains("Campaign mapping already exists");
            assertThat(summary.errors().get(6).message()).contains("budget must be non-negative");
            assertThat(summary.errors().get(7).message()).contains("Invalid budget number format");
        }
    }

    @Nested
    @DisplayName("Campaign Analytics Tests")
    class CampaignAnalyticsTests {

        @Test
        @DisplayName("Should successfully calculate campaign analytics and daily breakdown")
        void getCampaignAnalytics_Success() {
            UUID campaignId = UUID.randomUUID();
            LocalDate startDate = LocalDate.of(2026, 8, 1);
            LocalDate endDate = LocalDate.of(2026, 8, 2);

            when(campaignRepository.existsById(campaignId)).thenReturn(true);

            CampaignDailyAnalyticsEntity d1 = CampaignDailyAnalyticsEntity.builder()
                    .id(UUID.randomUUID())
                    .campaignId(campaignId)
                    .date(startDate)
                    .impressions(5000)
                    .clicks(100)
                    .spend(new BigDecimal("125.00"))
                    .conversions(5)
                    .build();

            CampaignDailyAnalyticsEntity d2 = CampaignDailyAnalyticsEntity.builder()
                    .id(UUID.randomUUID())
                    .campaignId(campaignId)
                    .date(endDate)
                    .impressions(5000)
                    .clicks(150)
                    .spend(new BigDecimal("187.50"))
                    .conversions(10)
                    .build();

            when(campaignDailyAnalyticsRepository.findByCampaignIdAndDateBetweenOrderByDateAsc(campaignId, startDate, endDate))
                    .thenReturn(List.of(d1, d2));

            CampaignAnalyticsResponse response = campaignService.getCampaignAnalytics(campaignId, startDate, endDate);

            assertThat(response).isNotNull();
            assertThat(response.campaignId()).isEqualTo(campaignId);
            assertThat(response.summary().totalImpressions()).isEqualTo(10000);
            assertThat(response.summary().totalClicks()).isEqualTo(250);
            assertThat(response.summary().totalSpend()).isEqualTo(new BigDecimal("312.50"));
            assertThat(response.summary().totalConversions()).isEqualTo(15);
            assertThat(response.summary().ctr()).isEqualTo(2.50);
            assertThat(response.summary().cpc()).isEqualTo(1.25);

            assertThat(response.dailyBreakdown()).hasSize(2);
            assertThat(response.dailyBreakdown().get(0).ctr()).isEqualTo(2.00);
            assertThat(response.dailyBreakdown().get(0).cpc()).isEqualTo(1.25);
            assertThat(response.dailyBreakdown().get(1).ctr()).isEqualTo(3.00);
            assertThat(response.dailyBreakdown().get(1).cpc()).isEqualTo(1.25);
        }

        @Test
        @DisplayName("Should safely handle zero impressions and zero clicks with 0.0 CTR and CPC")
        void getCampaignAnalytics_ZeroImpressionsAndClicks() {
            UUID campaignId = UUID.randomUUID();
            LocalDate startDate = LocalDate.of(2026, 8, 1);
            LocalDate endDate = LocalDate.of(2026, 8, 1);

            when(campaignRepository.existsById(campaignId)).thenReturn(true);

            CampaignDailyAnalyticsEntity d1 = CampaignDailyAnalyticsEntity.builder()
                    .id(UUID.randomUUID())
                    .campaignId(campaignId)
                    .date(startDate)
                    .impressions(0)
                    .clicks(0)
                    .spend(BigDecimal.ZERO)
                    .conversions(0)
                    .build();

            when(campaignDailyAnalyticsRepository.findByCampaignIdAndDateBetweenOrderByDateAsc(campaignId, startDate, endDate))
                    .thenReturn(List.of(d1));

            CampaignAnalyticsResponse response = campaignService.getCampaignAnalytics(campaignId, startDate, endDate);

            assertThat(response).isNotNull();
            assertThat(response.summary().totalImpressions()).isZero();
            assertThat(response.summary().totalClicks()).isZero();
            assertThat(response.summary().ctr()).isEqualTo(0.0);
            assertThat(response.summary().cpc()).isEqualTo(0.0);
            assertThat(response.dailyBreakdown().get(0).ctr()).isEqualTo(0.0);
            assertThat(response.dailyBreakdown().get(0).cpc()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when campaign does not exist")
        void getCampaignAnalytics_NotFound() {
            UUID campaignId = UUID.randomUUID();
            when(campaignRepository.existsById(campaignId)).thenReturn(false);

            assertThatThrownBy(() -> campaignService.getCampaignAnalytics(campaignId, null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Campaign not found with ID: " + campaignId);
        }
    }
}
