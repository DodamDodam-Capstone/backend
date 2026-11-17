package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.SmtpEmailVerificationSender;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.net.InetAddress;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PinEmailVerificationTests {
    @Autowired EmailVerificationIssuanceService issuance;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean SmtpEmailVerificationSender sender;
    AuthenticatedSession current;
    String email;

    @BeforeEach void setup() {
        clean();
        var account = UUID.randomUUID();
        email = account + "@example.invalid";
        var context = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email,session_version) VALUES(?,?,3)", account, email);
        jdbc.update("INSERT INTO session_security(id,account_id,session_version,setup_generation) VALUES(?,?,3,11)", context, account);
        current = new AuthenticatedSession(account, context);
    }
    @AfterEach void clean() {
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM guardian_pins");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest @ValueSource(strings={"PIN_SETUP","PIN_RESET"})
    void pinIssueUsesServerEmailContextGenerationAndLatestPinVersion(Purpose purpose) {
        if (purpose == Purpose.PIN_RESET) { pin(); }
        var request = new MockHttpServletRequest();
        var response = issue(request, purpose, null);
        assertThat(jdbc.queryForMap("SELECT * FROM email_verifications WHERE id=?", response.challengeId()))
                .containsEntry("email", email).containsEntry("setup_account_id", current.accountId())
                .containsEntry("setup_session_binding", current.securityContextId()).containsEntry("setup_generation", 11L)
                .containsEntry("pin_version_snapshot", purpose == Purpose.PIN_RESET ? 17L : null);
        verify(sender).send(eq(email), anyString(), eq(true));
        issuance.verifyResponseAccess(request);
    }

    @ParameterizedTest @CsvSource({"PIN_SETUP,true,PIN_ALREADY_SET", "PIN_RESET,false,PIN_NOT_SET"})
    void pinStateConflictsAre409(Purpose purpose, boolean exists, String code) {
        if (exists) { pin(); }
        assertThatThrownBy(() -> issue(new MockHttpServletRequest(), purpose, null))
                .isInstanceOfSatisfying(EmailVerificationException.class, e -> {
                    assertThat(e.status()).isEqualTo(409); assertThat(e.code()).isEqualTo(code);
                });
        verifyNoInteractions(sender);
    }

    @Test void suppliedEmailMustMatchCurrentAccount() {
        assertThatThrownBy(() -> issue(new MockHttpServletRequest(), Purpose.PIN_SETUP, "other@example.invalid"))
                .isInstanceOfSatisfying(EmailVerificationException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @ParameterizedTest @CsvSource({"LOGOUT,401", "LOCK,403", "EMAIL,403", "VERSION,401", "PIN_CREATED,403"})
    void changesDuringSmtpDoNotReturnUsableSetupProof(String change, int expected) {
        doAnswer(call -> { change(change); return null; }).when(sender).send(anyString(), anyString(), anyBoolean());
        assertThatThrownBy(() -> issue(new MockHttpServletRequest(), Purpose.PIN_SETUP, null))
                .isInstanceOfSatisfying(EmailVerificationException.class, e -> assertThat(e.status()).isEqualTo(expected));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NOT NULL", Long.class)).isEqualTo(1);
    }

    @Test void pinVersionChangeAfterServiceReturnIsRejectedByResponseCheck() {
        pin();
        var request = new MockHttpServletRequest();
        issue(request, Purpose.PIN_RESET, null);
        jdbc.update("UPDATE guardian_pins SET pin_version=18");
        assertThatThrownBy(() -> issuance.verifyResponseAccess(request))
                .isInstanceOfSatisfying(EmailVerificationException.class,
                        e -> assertThat(e.code()).isEqualTo("PIN_RESET_AUTHORIZATION_REQUIRED"));
    }

    @Test void responseReadFailureRemains503() {
        var request = new MockHttpServletRequest();
        issue(request, Purpose.PIN_SETUP, null);
        jdbc.execute("ALTER TABLE guardian_pins RENAME TO unavailable_guardian_pins");
        try { assertThatThrownBy(() -> issuance.verifyResponseAccess(request)).isInstanceOf(AuthStateUnavailableException.class); }
        finally { jdbc.execute("ALTER TABLE unavailable_guardian_pins RENAME TO guardian_pins"); }
    }

    private void pin() { jdbc.update("INSERT INTO guardian_pins(account_id,pin_hash,pin_version) VALUES(?,'synthetic-hash',17)", current.accountId()); }
    private com.dodamdodam.backend.auth.dto.VerificationIssueResponse issue(MockHttpServletRequest request, Purpose purpose, String suppliedEmail) {
        return issuance.issue(request, new IssueEmailVerificationRequest(purpose, suppliedEmail), current, InetAddress.ofLiteral("192.0.2.10"));
    }
    private void change(String change) {
        switch (change) {
            case "LOGOUT" -> jdbc.update("UPDATE session_security SET revoked_at=clock_timestamp()");
            case "LOCK" -> jdbc.update("UPDATE session_security SET setup_generation=12");
            case "EMAIL" -> jdbc.update("UPDATE accounts SET email='changed@example.invalid'");
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version=4");
            case "PIN_CREATED" -> pin();
            default -> throw new AssertionError(change);
        }
    }
}
