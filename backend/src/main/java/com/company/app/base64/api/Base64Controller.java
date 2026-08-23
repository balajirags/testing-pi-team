package com.company.app.base64.api;

import com.company.app.base64.api.dto.Base64EncodeRequest;
import com.company.app.base64.api.dto.Base64EncodeResponse;
import com.company.app.base64.service.Base64Service;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/base64")
public class Base64Controller {

    private final Base64Service base64Service;

    public Base64Controller(Base64Service base64Service) {
        this.base64Service = base64Service;
    }

    @PostMapping(value = "/encode", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Base64EncodeResponse encodeText(@Valid @RequestBody Base64EncodeRequest request) {
        return base64Service.encodeText(request);
    }
}
