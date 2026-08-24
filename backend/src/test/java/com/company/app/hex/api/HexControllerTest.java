package com.company.app.hex.api;

import com.company.app.campaign.exception.GlobalExceptionHandler;
import com.company.app.hex.api.dto.HexGenerateRequest;
import com.company.app.hex.api.dto.HexGenerateResponse;
import com.company.app.hex.service.HexService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HexController.class)
@Import(GlobalExceptionHandler.class)
class HexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HexService hexService;

    @Test
    @DisplayName("POST /api/v1/hex/generate - Valid request with default length returns 200 OK with 32-char hex")
    void generateHex_DefaultLength_Success() throws Exception {
        HexGenerateRequest request = new HexGenerateRequest("hello", null);
        HexGenerateResponse response = new HexGenerateResponse("hello", "4a8b2c9d0e1f3a5b7c9d1e3f5a7b9c1d", 32, Instant.now());

        when(hexService.generateHex(any(HexGenerateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/hex/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.text", is("hello")))
                .andExpect(jsonPath("$.hex", is("4a8b2c9d0e1f3a5b7c9d1e3f5a7b9c1d")))
                .andExpect(jsonPath("$.length", is(32)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/hex/generate - Custom length returns 200 OK with requested length")
    void generateHex_CustomLength_Success() throws Exception {
        HexGenerateRequest request = new HexGenerateRequest("test", 64);
        HexGenerateResponse response = new HexGenerateResponse("test", "4a8b2c9d0e1f3a5b7c9d1e3f5a7b9c1d4a8b2c9d0e1f3a5b7c9d1e3f5a7b9c1d", 64, Instant.now());

        when(hexService.generateHex(any(HexGenerateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/hex/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.text", is("test")))
                .andExpect(jsonPath("$.hex", hasLength(64)))
                .andExpect(jsonPath("$.length", is(64)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/hex/generate - Missing or null text returns 400 Bad Request ProblemDetail")
    void generateHex_NullText_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/hex/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"length\": 32}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/v1/hex/generate - Out of bounds length returns 400 Bad Request ProblemDetail")
    void generateHex_OutOfBoundsLength_Returns400() throws Exception {
        HexGenerateRequest request = new HexGenerateRequest("data", 6);
        when(hexService.generateHex(any(HexGenerateRequest.class)))
                .thenThrow(new IllegalArgumentException("length must be between 8 and 128"));

        mockMvc.perform(post("/api/v1/hex/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("length must be between 8 and 128")));
    }

    @Test
    @DisplayName("POST /api/v1/hex/generate - Non-even length returns 400 Bad Request ProblemDetail")
    void generateHex_NonEvenLength_Returns400() throws Exception {
        HexGenerateRequest request = new HexGenerateRequest("data", 15);
        when(hexService.generateHex(any(HexGenerateRequest.class)))
                .thenThrow(new IllegalArgumentException("length must be an even integer"));

        mockMvc.perform(post("/api/v1/hex/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("length must be an even integer")));
    }

    @Test
    @DisplayName("POST /api/v1/hex/generate - Invalid Content-Type returns 415 Unsupported Media Type")
    void generateHex_InvalidContentType_Returns415() throws Exception {
        mockMvc.perform(post("/api/v1/hex/generate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
