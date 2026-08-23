package com.company.app.base64.api;

import com.company.app.base64.api.dto.Base64EncodeRequest;
import com.company.app.base64.api.dto.Base64EncodeResponse;
import com.company.app.base64.service.Base64Service;
import com.company.app.campaign.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(Base64Controller.class)
@Import(GlobalExceptionHandler.class)
class Base64ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Base64Service base64Service;

    @Test
    @DisplayName("POST /api/v1/base64/encode - Standard ASCII text returns 200 OK and encoded result")
    void encode_StandardAscii_Success() throws Exception {
        Base64EncodeRequest request = new Base64EncodeRequest("Hello World");
        Base64EncodeResponse response = new Base64EncodeResponse("Hello World", "SGVsbG8gV29ybGQ=");

        when(base64Service.encodeText(any(Base64EncodeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/base64/encode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.originalText", is("Hello World")))
                .andExpect(jsonPath("$.encodedText", is("SGVsbG8gV29ybGQ=")));
    }

    @Test
    @DisplayName("POST /api/v1/base64/encode - UTF-8 text with emoji returns 200 OK and encoded result")
    void encode_Utf8AndEmoji_Success() throws Exception {
        Base64EncodeRequest request = new Base64EncodeRequest("Hello World! 🚀");
        Base64EncodeResponse response = new Base64EncodeResponse("Hello World! 🚀", "SGVsbG8gV29ybGQhIPCfmoA=");

        when(base64Service.encodeText(any(Base64EncodeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/base64/encode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.originalText", is("Hello World! 🚀")))
                .andExpect(jsonPath("$.encodedText", is("SGVsbG8gV29ybGQhIPCfmoA=")));
    }

    @Test
    @DisplayName("POST /api/v1/base64/encode - Empty string returns 200 OK and empty encoded text")
    void encode_EmptyString_Success() throws Exception {
        Base64EncodeRequest request = new Base64EncodeRequest("");
        Base64EncodeResponse response = new Base64EncodeResponse("", "");

        when(base64Service.encodeText(any(Base64EncodeRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/base64/encode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.originalText", is("")))
                .andExpect(jsonPath("$.encodedText", is("")));
    }

    @Test
    @DisplayName("POST /api/v1/base64/encode - Null or missing text field returns 400 Bad Request ProblemDetail")
    void encode_NullTextField_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/base64/encode")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("POST /api/v1/base64/encode - Invalid Content-Type returns 415 Unsupported Media Type")
    void encode_InvalidContentType_Returns415() throws Exception {
        mockMvc.perform(post("/api/v1/base64/encode")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Hello World"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
