package com.company.app.password.service;

import com.company.app.password.api.dto.PasswordGenerateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordServiceTest {

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }

    @Test
    @DisplayName("generatePassword - Default length 12 returns valid 12-char alphanumeric password")
    void generatePassword_DefaultLength() {
        PasswordGenerateResponse response = passwordService.generatePassword(12);

        assertThat(response).isNotNull();
        assertThat(response.length()).isEqualTo(12);
        assertThat(response.password()).hasSize(12);
        assertThat(response.password()).matches("^[a-zA-Z0-9]{12}$");
        assertThat(response.timestamp()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64})
    @DisplayName("generatePassword - Valid lengths return password of expected length")
    void generatePassword_ValidLengths(int length) {
        PasswordGenerateResponse response = passwordService.generatePassword(length);

        assertThat(response).isNotNull();
        assertThat(response.length()).isEqualTo(length);
        assertThat(response.password()).hasSize(length);
        assertThat(response.password()).matches("^[a-zA-Z0-9]+$");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7, 65, 100, -5})
    @DisplayName("generatePassword - Invalid length throws IllegalArgumentException")
    void generatePassword_InvalidLengths(int length) {
        assertThatThrownBy(() -> passwordService.generatePassword(length))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("length must be between 8 and 64");
    }
}
