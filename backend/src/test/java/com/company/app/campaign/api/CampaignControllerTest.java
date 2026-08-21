package com.company.app.campaign.api;

import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.UpdateCampaignRequest;
import com.company.app.campaign.domain.CampaignStatus;
import com.company.app.campaign.exception.DuplicateResourceException;
import com.company.app.campaign.exception.GlobalExceptionHandler;
import com.company.app.campaign.exception.InvalidStateTransitionException;
import com.company.app.campaign.exception.ResourceNotFoundException;
import com.company.app.campaign.service.CampaignService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                null,
                null,
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

    @Test
    @DisplayName("GET /api/v1/campaigns - Happy path returns 200 OK with paginated list")
    void listCampaigns_HappyPath() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID adAccountId = UUID.randomUUID();
        Instant now = Instant.now();

        CampaignResponse response = new CampaignResponse(
                campaignId,
                brandId,
                adAccountId,
                "Summer Promo",
                new BigDecimal("2500.00"),
                "USD",
                "GOOGLE",
                "goog_123",
                CampaignStatus.DRAFT,
                null,
                null,
                now,
                now
        );

        Page<CampaignResponse> page = new PageImpl<>(List.of(response));
        when(campaignService.listCampaigns(eq(brandId), eq("GOOGLE"), eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/campaigns")
                        .param("brandId", brandId.toString())
                        .param("channel", "GOOGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(campaignId.toString())))
                .andExpect(jsonPath("$.content[0].channel", is("GOOGLE")));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id} - Happy path returns 200 OK with campaign")
    void getCampaignById_HappyPath() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID adAccountId = UUID.randomUUID();
        Instant now = Instant.now();

        CampaignResponse response = new CampaignResponse(
                campaignId,
                brandId,
                adAccountId,
                "Summer Promo",
                new BigDecimal("2500.00"),
                "USD",
                "GOOGLE",
                "goog_123",
                CampaignStatus.DRAFT,
                null,
                null,
                now,
                now
        );

        when(campaignService.getCampaignById(campaignId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/campaigns/{id}", campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(campaignId.toString())))
                .andExpect(jsonPath("$.name", is("Summer Promo")));
    }

    @Test
    @DisplayName("GET /api/v1/campaigns/{id} - Not found returns 404 ProblemDetail")
    void getCampaignById_NotFound() throws Exception {
        UUID campaignId = UUID.randomUUID();
        when(campaignService.getCampaignById(campaignId))
                .thenThrow(new ResourceNotFoundException("Campaign not found with ID: " + campaignId));

        mockMvc.perform(get("/api/v1/campaigns/{id}", campaignId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.detail", containsString("Campaign not found")));
    }

    @Test
    @DisplayName("PUT /api/v1/campaigns/{id} - Happy path returns 200 OK with updated campaign")
    void updateCampaign_HappyPath() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID brandId = UUID.randomUUID();
        UUID adAccountId = UUID.randomUUID();
        Instant now = Instant.now();

        UpdateCampaignRequest request = new UpdateCampaignRequest(
                "Updated Name",
                new BigDecimal("10000.00"),
                "USD",
                CampaignStatus.ACTIVE,
                null,
                null
        );

        CampaignResponse response = new CampaignResponse(
                campaignId,
                brandId,
                adAccountId,
                "Updated Name",
                new BigDecimal("10000.00"),
                "USD",
                "GOOGLE",
                "goog_123",
                CampaignStatus.ACTIVE,
                null,
                null,
                now,
                now
        );

        when(campaignService.updateCampaign(eq(campaignId), any(UpdateCampaignRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/campaigns/{id}", campaignId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(campaignId.toString())))
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.budget", is(10000.00)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    @Test
    @DisplayName("PUT /api/v1/campaigns/{id} - Negative budget returns 400 Bad Request ProblemDetail")
    void updateCampaign_NegativeBudget() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UpdateCampaignRequest request = new UpdateCampaignRequest(
                "Updated Name",
                new BigDecimal("-500.00"),
                "USD",
                CampaignStatus.ACTIVE,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/campaigns/{id}", campaignId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request Content")))
                .andExpect(jsonPath("$.invalidParams.budget", is("budget must be non-negative")));
    }

    @Test
    @DisplayName("PUT /api/v1/campaigns/{id} - Invalid state transition returns 400 Bad Request ProblemDetail")
    void updateCampaign_InvalidStateTransition() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UpdateCampaignRequest request = new UpdateCampaignRequest(
                null,
                null,
                null,
                CampaignStatus.DRAFT,
                null,
                null
        );

        when(campaignService.updateCampaign(eq(campaignId), any(UpdateCampaignRequest.class)))
                .thenThrow(new InvalidStateTransitionException("Cannot transition campaign status from COMPLETED to DRAFT"));

        mockMvc.perform(put("/api/v1/campaigns/{id}", campaignId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Request State or Argument")))
                .andExpect(jsonPath("$.detail", containsString("Cannot transition campaign status from COMPLETED to DRAFT")));
    }

    @Test
    @DisplayName("DELETE /api/v1/campaigns/{id} - Happy path returns 204 No Content")
    void deleteCampaign_HappyPath() throws Exception {
        UUID campaignId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/campaigns/{id}", campaignId))
                .andExpect(status().isNoContent());

        verify(campaignService).deleteCampaign(campaignId);
    }

    @Test
    @DisplayName("DELETE /api/v1/campaigns/{id} - Not found returns 404 ProblemDetail")
    void deleteCampaign_NotFound() throws Exception {
        UUID campaignId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Campaign not found with ID: " + campaignId))
                .when(campaignService).deleteCampaign(campaignId);

        mockMvc.perform(delete("/api/v1/campaigns/{id}", campaignId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.detail", containsString("Campaign not found")));
    }
}
