package com.company.app.campaign;

import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CreateCampaignRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
