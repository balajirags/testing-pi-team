package com.company.app.campaign;

import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.UpdateCampaignRequest;
import com.company.app.campaign.domain.AdAccountEntity;
import com.company.app.campaign.domain.BrandEntity;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.repository.AdAccountRepository;
import com.company.app.campaign.repository.BrandRepository;
import com.company.app.campaign.repository.CampaignRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CampaignIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private AdAccountRepository adAccountRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    private UUID brandId;
    private UUID adAccountId;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
        adAccountRepository.deleteAll();
        brandRepository.deleteAll();

        brandId = UUID.randomUUID();
        BrandEntity brand = BrandEntity.builder()
                .id(brandId)
                .name("Acme Corp")
                .build();
        brandRepository.save(brand);

        adAccountId = UUID.randomUUID();
        AdAccountEntity adAccount = AdAccountEntity.builder()
                .id(adAccountId)
                .brandId(brandId)
                .name("Meta Ad Account 1")
                .build();
        adAccountRepository.save(adAccount);
    }

    @Test
    @DisplayName("End to end campaign creation via REST endpoint")
    void endToEnd_CreateCampaign() throws Exception {
        CreateCampaignRequest request = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.brandId", is(brandId.toString())))
                .andExpect(jsonPath("$.adAccountId", is(adAccountId.toString())))
                .andExpect(jsonPath("$.name", is("Q1 Retargeting")))
                .andExpect(jsonPath("$.budget", is(5000.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.channel", is("META")))
                .andExpect(jsonPath("$.externalCampaignId", is("meta_12345")))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        // Attempting duplicate creation should yield 409
        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Duplicate Resource")));
    }

    @Test
    @DisplayName("End to end list, filter and get campaign by ID via REST endpoints")
    void endToEnd_ListFilterAndGetCampaign() throws Exception {
        // Create 2 campaigns: one GOOGLE, one META
        CreateCampaignRequest campaign1 = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Google Search",
                new BigDecimal("1000.00"),
                "USD",
                "GOOGLE",
                "goog_100"
        );
        CreateCampaignRequest campaign2 = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Meta Retargeting",
                new BigDecimal("2000.00"),
                "USD",
                "META",
                "meta_200"
        );

        String response1Str = mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campaign1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(campaign2)))
                .andExpect(status().isCreated());

        CampaignResponse response1 = objectMapper.readValue(response1Str, CampaignResponse.class);

        // 1. List all
        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // 2. Filter by channel=GOOGLE
        mockMvc.perform(get("/api/v1/campaigns")
                        .param("channel", "GOOGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Google Search")));

        // 3. Get by ID happy path
        mockMvc.perform(get("/api/v1/campaigns/{id}", response1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(response1.id().toString())))
                .andExpect(jsonPath("$.name", is("Google Search")));

        // 4. Get by ID not found
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/campaigns/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("End to end campaign update via REST endpoint")
    void endToEnd_UpdateCampaign() throws Exception {
        CreateCampaignRequest createRequest = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Initial Campaign",
                new BigDecimal("1000.00"),
                "USD",
                "GOOGLE",
                "goog_300"
        );

        String createRespStr = mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CampaignResponse created = objectMapper.readValue(createRespStr, CampaignResponse.class);

        // 1. Update budget and status to ACTIVE
        UpdateCampaignRequest update1 = new UpdateCampaignRequest(
                "Updated Campaign Name",
                new BigDecimal("10000.00"),
                "USD",
                CampaignStatus.ACTIVE,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/campaigns/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Campaign Name")))
                .andExpect(jsonPath("$.budget", is(10000.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // 2. Transition status to COMPLETED
        UpdateCampaignRequest update2 = new UpdateCampaignRequest(
                null, null, null, CampaignStatus.COMPLETED, null, null
        );

        mockMvc.perform(put("/api/v1/campaigns/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        // 3. Attempt invalid status transition COMPLETED -> DRAFT
        UpdateCampaignRequest invalidTransition = new UpdateCampaignRequest(
                null, null, null, CampaignStatus.DRAFT, null, null
        );

        mockMvc.perform(put("/api/v1/campaigns/{id}", created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransition)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request State or Argument")));

        // 4. Update non-existent campaign -> 404
        UUID missingId = UUID.randomUUID();
        mockMvc.perform(put("/api/v1/campaigns/{id}", missingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("End to end campaign soft-delete and list filtering via REST endpoints")
    void endToEnd_DeleteCampaign() throws Exception {
        CreateCampaignRequest createRequest = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Campaign To Delete",
                new BigDecimal("1500.00"),
                "USD",
                "GOOGLE",
                "goog_400"
        );

        String createRespStr = mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CampaignResponse created = objectMapper.readValue(createRespStr, CampaignResponse.class);

        // 1. Delete campaign -> 204 No Content
        mockMvc.perform(delete("/api/v1/campaigns/{id}", created.id()))
                .andExpect(status().isNoContent());

        // 2. Fetch campaign by ID -> returns 200 OK with ARCHIVED status
        mockMvc.perform(get("/api/v1/campaigns/{id}", created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(created.id().toString())))
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        // 3. Default list campaigns -> excludes ARCHIVED campaign
        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));

        // 4. List campaigns with status=ARCHIVED -> includes archived campaign
        mockMvc.perform(get("/api/v1/campaigns")
                        .param("status", "ARCHIVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(created.id().toString())));

        // 5. Delete non-existent campaign -> 404 Not Found
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/campaigns/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("End to end batch CSV import with valid and invalid rows")
    void endToEnd_ImportCampaignsFromCsv() throws Exception {
        UUID invalidBrandId = UUID.randomUUID();
        String csvContent = "name,brand_id,ad_account_id,budget,currency,channel,external_campaign_id\n" +
                "Batch Campaign 1," + brandId + "," + adAccountId + ",1200.00,USD,META,batch_meta_1\n" +
                "Batch Campaign 2," + brandId + "," + adAccountId + ",3400.00,USD,GOOGLE,batch_goog_2\n" +
                "Bad Brand Campaign," + invalidBrandId + "," + adAccountId + ",500.00,USD,TIKTOK,batch_tiktok_3";

        MockMultipartFile file = new MockMultipartFile("file", "campaigns.csv", "text/csv", csvContent.getBytes());

        mockMvc.perform(multipart("/api/v1/campaigns/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(3)))
                .andExpect(jsonPath("$.created", is(2)))
                .andExpect(jsonPath("$.failed", is(1)))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].rowNumber", is(4)));

        // Verify that 2 valid campaigns were created in DB and can be listed
        mockMvc.perform(get("/api/v1/campaigns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }
}
