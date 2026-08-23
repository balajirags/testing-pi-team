package com.company.app.hash.service;

import com.company.app.hash.api.dto.RandomHashResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RandomHashServiceTest {

    private RandomHashService randomHashService;

    @BeforeEach
    void setUp() {
        randomHashService = new RandomHashService();
    }

    @Test
    @DisplayName("generateRandomHash returns valid SHA-256 hex string with status SUCCESS")
    void shouldGenerateValidRandomHashResponse() {
        RandomHashResponse response = randomHashService.generateRandomHash();

        assertThat(response).isNotNull();
        assertThat(response.hash()).isNotNull().hasSize(64).matches("^[a-f0-9]{64}$");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    @DisplayName("generateRandomHash produces unique hash values on consecutive calls")
    void shouldProduceUniqueHashesOnConsecutiveCalls() {
        RandomHashResponse response1 = randomHashService.generateRandomHash();
        RandomHashResponse response2 = randomHashService.generateRandomHash();

        assertThat(response1.hash()).isNotEqualTo(response2.hash());
    }

    @Test
    @DisplayName("computeSha256 throws IllegalStateException when algorithm is invalid")
    void shouldThrowIllegalStateExceptionOnInvalidAlgorithm() {
        assertThatThrownBy(() -> randomHashService.computeSha256("test", "INVALID_ALGO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 algorithm not available");
    }
}
