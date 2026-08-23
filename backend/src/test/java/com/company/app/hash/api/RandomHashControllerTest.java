package com.company.app.hash.api;

import com.company.app.hash.api.dto.RandomHashResponse;
import com.company.app.hash.service.RandomHashService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RandomHashController.class)
class RandomHashControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RandomHashService randomHashService;

    @Test
    @DisplayName("GET /api/v1/random-hash returns 200 OK with valid JSON response")
    void shouldReturnRandomHashResponse() throws Exception {
        String sampleHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        Instant now = Instant.now();
        RandomHashResponse mockResponse = new RandomHashResponse(sampleHash, "SUCCESS", now);

        given(randomHashService.generateRandomHash()).willReturn(mockResponse);

        mockMvc.perform(get("/api/v1/random-hash"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.hash").value(sampleHash))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
