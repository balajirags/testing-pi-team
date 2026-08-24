package com.company.app.hex.api;

import com.company.app.hex.api.dto.HexGenerateRequest;
import com.company.app.hex.api.dto.HexGenerateResponse;
import com.company.app.hex.service.HexService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hex")
public class HexController {

    private final HexService hexService;

    public HexController(HexService hexService) {
        this.hexService = hexService;
    }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public HexGenerateResponse generateHex(@Valid @RequestBody HexGenerateRequest request) {
        return hexService.generateHex(request);
    }
}
