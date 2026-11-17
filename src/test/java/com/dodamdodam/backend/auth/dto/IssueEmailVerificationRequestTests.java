package com.dodamdodam.backend.auth.dto;

import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IssueEmailVerificationRequestTests {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void openValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void normalizesEmailWithoutRemovingDotsOrPlusSuffix() {
        var request = new IssueEmailVerificationRequest(Purpose.SIGNUP, "\u00a0\u2003 A.B+Tag@EXAMPLE.COM \t");
        assertThat(request.normalized(validator).email()).isEqualTo("a.b+tag@example.com");
        assertThat(request.email()).startsWith("\u00a0");
    }

    @Test
    @ResourceLock("java.util.Locale.default")
    void normalizationDoesNotDependOnTurkishDefaultLocale() {
        var previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(new IssueEmailVerificationRequest(Purpose.SIGNUP, "I@EXAMPLE.COM")
                    .normalized(validator).email()).isEqualTo("i@example.com");
        }
        finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @EnumSource(Purpose.class)
    void parsesEveryDocumentedPurpose(Purpose purpose) {
        var body = JSON.createObjectNode().put("purpose", purpose.name()).put("email", "USER@example.com");
        var parsed = IssueEmailVerificationRequest.fromJson(body).normalized(validator);
        assertThat(parsed.purpose()).isEqualTo(purpose);
        assertThat(parsed.email()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIGNUP", "RESET_PASSWORD"})
    void publicPurposesRequireEmail(String purpose) {
        var body = JSON.createObjectNode().put("purpose", purpose);
        rejects(body, "email");
        body.putNull("email");
        rejects(body, "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIN_SETUP", "PIN_RESET"})
    void pinPurposesAllowOmittedEmailButRejectExplicitNull(String purpose) {
        var body = JSON.createObjectNode().put("purpose", purpose);
        assertThat(IssueEmailVerificationRequest.fromJson(body).normalized(validator).email()).isNull();
        body.putNull("email");
        rejects(body, "email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "true", "1", "\"text\""})
    void rejectsNonObjectBody(String json) {
        rejects(JSON.readTree(json), "body");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"purpose\":null}", "{\"purpose\":1}", "{\"purpose\":true}",
            "{\"purpose\":\"signup\"}", "{\"purpose\":\" SIGNUP\"}", "{\"purpose\":\"OTHER\"}"})
    void rejectsMissingNonStringOrUnknownPurpose(String json) {
        rejects(JSON.readTree(json), "purpose");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "false", "[]", "{}"})
    void doesNotCoerceEmailToString(String json) {
        var body = JSON.createObjectNode().put("purpose", "SIGNUP");
        body.set("email", JSON.readTree(json));
        rejects(body, "email");
    }

    @Test
    void rejectsUndocumentedFieldsAndNullJavaBody() {
        var body = JSON.createObjectNode().put("purpose", "SIGNUP").put("email", "user@example.com")
                .put("accountId", "untrusted");
        rejects(body, "body");
        rejects(null, "body");
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    void rejectsInvalidEmailAfterNormalization(String email) {
        var failure = assertThrows(EmailVerificationException.class,
                () -> new IssueEmailVerificationRequest(Purpose.SIGNUP, email).normalized(validator));
        assertValidation(failure, "email");
        if (!email.isEmpty()) {
            assertThat(failure.getMessage()).doesNotContain(email);
        }
    }

    static Stream<String> invalidEmails() {
        return Stream.of("", "   ", "user", "a@@example.com", "a b@example.com", "a@exa mple.com",
                "a..b@example.com", "user@-example.com", "사용자@example.com", "user@예시.com",
                "user\u0000@example.com", "user\n@example.com", "a".repeat(64) + "@"
                        + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62));
    }

    @Test
    void accepts254CharactersAfterTrimmingAndRejectsMissingPurposeInDirectConstruction() {
        var email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertThat(email).hasSize(254);
        assertThat(new IssueEmailVerificationRequest(Purpose.SIGNUP, " " + email + " ")
                .normalized(validator).email()).isEqualTo(email);
        assertValidation(assertThrows(EmailVerificationException.class,
                () -> new IssueEmailVerificationRequest(null, email).normalized(validator)), "purpose");
    }

    private static void rejects(tools.jackson.databind.JsonNode body, String field) {
        assertValidation(assertThrows(EmailVerificationException.class,
                () -> IssueEmailVerificationRequest.fromJson(body).normalized(validator)), field);
    }

    private static void assertValidation(EmailVerificationException failure, String field) {
        assertThat(failure.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(failure.fields()).singleElement().satisfies(error -> {
            assertThat(error.field()).isEqualTo(field);
            assertThat(error.reason()).isNotBlank();
        });
    }
}
