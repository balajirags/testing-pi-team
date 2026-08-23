package com.company.app.base64.service;

import com.company.app.base64.api.dto.Base64EncodeRequest;
import com.company.app.base64.api.dto.Base64EncodeResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class Base64Service {

    public Base64EncodeResponse encodeText(Base64EncodeRequest request) {
        if (request == null || request.text() == null) {
            throw new IllegalArgumentException("Text must not be null");
        }
        String encoded = Base64.getEncoder().encodeToString(request.text().getBytes(StandardCharsets.UTF_8));
        return new Base64EncodeResponse(request.text(), encoded);
    }
}
