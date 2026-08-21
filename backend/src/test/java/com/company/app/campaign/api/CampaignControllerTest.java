package com.company.app.campaign.api;

import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.GlobalExceptionHandler;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.service.CampaignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CampaignController.class)
@Import(GlobalExceptionHandler.class)
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CampaignService campaignService;

    @Test
    @DisplayName("POST /api/v1/campaigns - Happy Path returns 201 Created and campaign entity")
    void createCampaign_HappyPath() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID adAccountId = UUID.randomUUID();
        Instant now = Instant.now();

        CreateCampaignRequest request = new CreateCampaignRequest(
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        CampaignResponse response = new CampaignResponse(
                campaignId,
                brandId,
                adAccountId,
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345",
                CampaignStatus.DRAFT,
                now,
                now
        );

        when(campaignService.createCampaign(any(CreateCampaignRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(campaignId.toString())))
                .andExpect(jsonPath("$.brandId", is(brandId.toString())))
                .andExpect(jsonPath("$.adAccountId", is(adAccountId.toString())))
                .andExpect(jsonPath("$.name", is("Q1 Retargeting")))
                .andExpect(jsonPath("$.budget", is(5000.00)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.channel", is("META")))
                .andExpect(jsonPath("$.externalCampaignId", is("meta_12345")))
                .andExpect(jsonPath("$.status", is("DRAFT")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns - Missing mandatory fields returns 400 Bad Request with ProblemDetail")
    void createCampaign_MissingMandatoryFields() throws Exception {
        // Missing name, channel, externalCampaignId
        CreateCampaignRequest request = new CreateCampaignRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "",
                new BigDecimal("5000.00"),
                "USD",
                "",
                ""
        );

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request Content")))
                .andExpect(jsonPath("$.invalidParams.name", is("name is required")))
                .andExpect(jsonPath("$.invalidParams.channel", is("channel is required")))
                .andExpect(jsonPath("$.invalidParams.externalCampaignId", is("externalCampaignId is required")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns - Non-existent brand or ad account returns 404 Not Found ProblemDetail")
    void createCampaign_InvalidAssociation() throws Exception {
        CreateCampaignRequest request = new CreateCampaignRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        when(campaignService.createCampaign(any(CreateCampaignRequest.class)))
                .thenThrow(new ResourceNotFoundException("Brand not found with ID: " + request.brandId()));

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.detail", containsString("Brand not found")));
    }

    @Test
    @DisplayName("POST /api/v1/campaigns - Duplicate channel/external ID returns 409 Conflict ProblemDetail")
    void createCampaign_DuplicateMapping() throws Exception {
        CreateCampaignRequest request = new CreateCampaignRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Q1 Retargeting",
                new BigDecimal("5000.00"),
                "USD",
                "META",
                "meta_12345"
        );

        when(campaignService.createCampaign(any(CreateCampaignRequest.class)))
                .thenThrow(new DuplicateResourceException("Campaign mapping already exists for channel 'META' and external campaign ID 'meta_12345'"));

        mockMvc.perform(post("/api/v1/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Duplicate Resource")))
                .andExpect(jsonPath("$.detail", containsString("Campaign mapping already exists")));
    }
}
