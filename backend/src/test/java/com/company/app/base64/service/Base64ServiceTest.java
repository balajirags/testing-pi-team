package com.company.app.base64.service;

import com.company.app.base64.api.dto.Base64EncodeRequest;
import com.company.app.base64.api.dto.Base64EncodeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base64ServiceTest {

    private Base64Service base64Service;

    @BeforeEach
    void setUp() {
        base64Service = new Base64Service();
    }

    @Test
    @DisplayName("encodeText - Standard ASCII string returns correct Base64 encoding")
    void encodeText_StandardAscii() {
        Base64EncodeRequest request = new Base64EncodeRequest("Hello World");
        Base64EncodeResponse response = base64Service.encodeText(request);

        assertThat(response).isNotNull();
        assertThat(response.originalText()).isEqualTo("Hello World");
        assertThat(response.encodedText()).isEqualTo("SGVsbG8gV29ybGQ=");
    }

    @Test
    @DisplayName("encodeText - UTF-8 string with special chars and emoji returns correct Base64 encoding")
    void encodeText_Utf8AndEmoji() {
        Base64EncodeRequest request = new Base64EncodeRequest("Hello World! 🚀");
        Base64EncodeResponse response = base64Service.encodeText(request);

        assertThat(response).isNotNull();
        assertThat(response.originalText()).isEqualTo("Hello World! 🚀");
        assertThat(response.encodedText()).isEqualTo("SGVsbG8gV29ybGQhIPCfmoA=");
    }

    @Test
    @DisplayName("encodeText - Empty string returns empty string Base64 encoding")
    void encodeText_EmptyString() {
        Base64EncodeRequest request = new Base64EncodeRequest("");
        Base64EncodeResponse response = base64Service.encodeText(request);

        assertThat(response).isNotNull();
        assertThat(response.originalText()).isEqualTo("");
        assertThat(response.encodedText()).isEqualTo("");
    }

    @Test
    @DisplayName("encodeText - Null request or null text throws IllegalArgumentException")
    void encodeText_NullInputThrows() {
        assertThatThrownBy(() -> base64Service.encodeText(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Text must not be null");

        Base64EncodeRequest requestWithNullText = new Base64EncodeRequest(null);
        assertThatThrownBy(() -> base64Service.encodeText(requestWithNullText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Text must not be null");
    }
}
