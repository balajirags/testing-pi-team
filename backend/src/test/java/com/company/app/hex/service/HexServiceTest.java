package com.company.app.hex.service;

import com.company.app.hex.api.dto.HexGenerateRequest;
import com.company.app.hex.api.dto.HexGenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HexServiceTest {

    private HexService hexService;

    @BeforeEach
    void setUp() {
        hexService = new HexService();
    }

    @Test
    @DisplayName("generateHex - Default length 32 returns valid 32-char hex string")
    void generateHex_DefaultLength() {
        HexGenerateRequest request = new HexGenerateRequest("hello", null);
        HexGenerateResponse response = hexService.generateHex(request);

        assertThat(response).isNotNull();
        assertThat(response.text()).isEqualTo("hello");
        assertThat(response.length()).isEqualTo(32);
        assertThat(response.hex()).hasSize(32);
        assertThat(response.hex()).matches("^[0-9a-f]{32}$");
        assertThat(response.timestamp()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64, 128})
    @DisplayName("generateHex - Valid even lengths return expected hex length")
    void generateHex_ValidLengths(int length) {
        HexGenerateRequest request = new HexGenerateRequest("test", length);
        HexGenerateResponse response = hexService.generateHex(request);

        assertThat(response).isNotNull();
        assertThat(response.text()).isEqualTo("test");
        assertThat(response.length()).isEqualTo(length);
        assertThat(response.hex()).hasSize(length);
        assertThat(response.hex()).matches("^[0-9a-f]+$");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, 130, 200, -2})
    @DisplayName("generateHex - Length out of bounds throws IllegalArgumentException")
    void generateHex_OutOfBoundsLength_ThrowsException(int length) {
        HexGenerateRequest request = new HexGenerateRequest("test", length);

        assertThatThrownBy(() -> hexService.generateHex(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("length must be between 8 and 128");
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 15, 33, 65, 127})
    @DisplayName("generateHex - Odd length throws IllegalArgumentException")
    void generateHex_OddLength_ThrowsException(int length) {
        HexGenerateRequest request = new HexGenerateRequest("test", length);

        assertThatThrownBy(() -> hexService.generateHex(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("length must be an even integer");
    }

    @Test
    @DisplayName("generateHex - Null request or null text throws IllegalArgumentException")
    void generateHex_NullText_ThrowsException() {
        assertThatThrownBy(() -> hexService.generateHex(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("text must not be null");

        HexGenerateRequest requestWithNullText = new HexGenerateRequest(null, 32);
        assertThatThrownBy(() -> hexService.generateHex(requestWithNullText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("text must not be null");
    }
}
