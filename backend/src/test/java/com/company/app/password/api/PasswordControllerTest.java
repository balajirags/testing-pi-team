package com.company.app.password.api;

import com.company.app.campaign.exception.GlobalExceptionHandler;
import com.company.app.password.api.dto.PasswordGenerateResponse;
import com.company.app.password.service.PasswordService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordController.class)
@Import(GlobalExceptionHandler.class)
class PasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordService passwordService;

    @Test
    @DisplayName("GET /api/v1/passwords/generate - Default length returns 200 OK with length 12 password")
    void generatePassword_DefaultLength_Success() throws Exception {
        PasswordGenerateResponse response = new PasswordGenerateResponse("aB3k9XmP2vQ1", 12, Instant.now());
        when(passwordService.generatePassword(12)).thenReturn(response);

        mockMvc.perform(get("/api/v1/passwords/generate"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.password", is("aB3k9XmP2vQ1")))
                .andExpect(jsonPath("$.length", is(12)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/passwords/generate?length=16 - Custom length returns 200 OK")
    void generatePassword_CustomLength_Success() throws Exception {
        PasswordGenerateResponse response = new PasswordGenerateResponse("aB3k9XmP2vQ1aB3k", 16, Instant.now());
        when(passwordService.generatePassword(16)).thenReturn(response);

        mockMvc.perform(get("/api/v1/passwords/generate").param("length", "16"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.password", hasLength(16)))
                .andExpect(jsonPath("$.length", is(16)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/passwords/generate?length=7 - Length below minimum returns 400 Bad Request ProblemDetail")
    void generatePassword_LengthBelowMin_Returns400() throws Exception {
        when(passwordService.generatePassword(7))
                .thenThrow(new IllegalArgumentException("length must be between 8 and 64"));

        mockMvc.perform(get("/api/v1/passwords/generate").param("length", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("length must be between 8 and 64")));
    }

    @Test
    @DisplayName("GET /api/v1/passwords/generate?length=65 - Length above maximum returns 400 Bad Request ProblemDetail")
    void generatePassword_LengthAboveMax_Returns400() throws Exception {
        when(passwordService.generatePassword(65))
                .thenThrow(new IllegalArgumentException("length must be between 8 and 64"));

        mockMvc.perform(get("/api/v1/passwords/generate").param("length", "65"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.detail", is("length must be between 8 and 64")));
    }

    @Test
    @DisplayName("GET /api/v1/passwords/generate?length=invalid - Non-numeric length returns 400 Bad Request ProblemDetail")
    void generatePassword_InvalidType_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/passwords/generate").param("length", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }
}
