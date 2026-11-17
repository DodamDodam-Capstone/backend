package com.dodamdodam.backend.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Locale;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class LoginRequestTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configureValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void normalizesUnicodeOuterSpaceAndRootCaseWithoutChangingProviderSuffix() {
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            var input = parse("\u00a0 I.User+Tag@EXAMPLE.COM \u2003", "  synthetic-password  ");
            assertThat(input.email()).isEqualTo("i.user+tag@example.com");
            assertThat(input.password()).isEqualTo("  synthetic-password  ");
            assertThat(input.toString()).doesNotContain("i.user", "synthetic-password");
        }
        finally {
            Locale.setDefault(original);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"x", " ", "\n", "é", "e\u0301", "🙂"})
    void preservesExistingNonemptyPasswordExactly(String password) {
        assertThat(parse("u@example.invalid", password).password()).isEqualTo(password);
    }

    @Test
    void doesNotApplyNewPasswordLengthOrByteLimitToExistingPassword() {
        var password = "🙂".repeat(1024);
        assertThat(parse("u@example.invalid", password).password()).isEqualTo(password);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"bad-email", "한글@example.invalid", "u@exam ple.invalid", "u@example.invalid\u0000"})
    void rejectsInvalidEmailWithoutEchoingInput(String email) {
        assertThatThrownBy(() -> parse(email, "synthetic-password"))
                .isInstanceOfSatisfying(PasswordLoginException.class, error -> {
                    assertThat(error.status()).isEqualTo(400);
                    assertThat(error.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(error.fields().getFirst().field()).isEqualTo("email");
                    assertThat(error.getMessage()).doesNotContain("synthetic-password", "example.invalid");
                });
    }

    @Test
    void accepts254CharactersAfterNormalizationAndRejects255() {
        var email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(57) + ".com";
        assertThat(email).hasSize(254);
        assertThat(parse(email.toUpperCase(Locale.ROOT), "x").email()).isEqualTo(email);
        assertThatThrownBy(() -> parse("a" + email, "x")).isInstanceOf(PasswordLoginException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\ud800", "\udfff", "x\ud800y", "\ud800\ud800"})
    void rejectsEmptyOrUnpairedSurrogatePassword(String password) {
        assertThatThrownBy(() -> parse("u@example.invalid", password))
                .isInstanceOfSatisfying(PasswordLoginException.class, error -> {
                    assertThat(error.status()).isEqualTo(400);
                    assertThat(error.fields().getFirst().field()).isEqualTo("password");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "\"text\"", "{}", "{\"email\":\"u@example.invalid\"}",
            "{\"email\":\"u@example.invalid\",\"password\":null}", "{\"email\":42,\"password\":\"x\"}",
            "{\"email\":\"u@example.invalid\",\"password\":123}",
            "{\"email\":\"u@example.invalid\",\"password\":\"x\",\"rememberMe\":true}"})
    void rejectsMissingNullWrongTypeAndUnknownFields(String json) {
        assertThatThrownBy(() -> LoginRequest.fromJson(JSON.readTree(json), validator))
                .isInstanceOf(PasswordLoginException.class);
    }

    /** 직렬화 단계에서 보정하지 않은 문자열을 실제 JsonNode 입력으로 전달한다. */
    private LoginRequest parse(String email, String password) {
        var json = JSON.createObjectNode();
        if (email == null) {
            json.putNull("email");
        }
        else {
            json.put("email", email);
        }
        json.put("password", password);
        return LoginRequest.fromJson(json, validator);
    }
}
