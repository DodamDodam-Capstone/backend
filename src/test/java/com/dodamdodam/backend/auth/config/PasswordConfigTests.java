package com.dodamdodam.backend.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordConfigTests {
    @Test
    void usesSaltedVersionedHashesAndRejectsWrongPassword() {
        var encoder = new PasswordConfig().passwordEncoder();
        var first = encoder.encode("synthetic-password");
        var second = encoder.encode("synthetic-password");
        assertThat(first).startsWith("{pbkdf2-sha256-v1}").isNotEqualTo(second);
        assertThat(encoder.matches("synthetic-password", first)).isTrue();
        assertThat(encoder.matches("synthetic-wrong-password", first)).isFalse();
    }

    @Test
    void preservesUnicodeSpaceAndLongExistingInputsWithoutTruncationOrNormalization() {
        var encoder = new PasswordConfig().passwordEncoder();
        var password = " 🙂é ".repeat(512);
        var hash = encoder.encode(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches(password.strip(), hash)).isFalse();
        assertThat(encoder.matches(password.replace("é", "e\u0301"), hash)).isFalse();
        assertThat(encoder.matches(password + "x", hash)).isFalse();
    }
}
