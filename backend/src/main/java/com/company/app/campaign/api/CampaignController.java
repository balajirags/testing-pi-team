package com.company.app.campaign.api;

import com.company.app.campaign.api.dto.CampaignResponse;
import com.company.app.campaign.api.dto.CreateCampaignRequest;
import com.company.app.campaign.api.dto.UpdateCampaignRequest;
import com.company.app.campaign.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CampaignResponse>> listCampaigns(
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String channel,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CampaignResponse> campaigns = campaignService.listCampaigns(brandId, channel, pageable);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable UUID id) {
        CampaignResponse response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignRequest request) {
        CampaignResponse response = campaignService.updateCampaign(id, request);
        return ResponseEntity.ok(response);
    }
}
