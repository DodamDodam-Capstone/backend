package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.infrastructure.*;
import com.dodamdodam.backend.auth.exception.*;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.net.InetAddress;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailVerificationIssuanceTests {
    static final String EMAIL = "public@example.invalid";
    @Autowired EmailVerificationIssuanceService issuance;
    @Autowired JdbcTemplate jdbc;
    @Autowired EmailVerificationKeys keys;
    @MockitoBean SmtpEmailVerificationSender sender;
    @BeforeEach @AfterEach void clean() {
        jdbc.update("DELETE FROM email_verifications");
        jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @CsvSource({"SIGNUP,false,true", "SIGNUP,true,false", "RESET_PASSWORD,false,false", "RESET_PASSWORD,true,true"})
    void publicEligibilityAndDecoyHaveSamePublicResult(Purpose purpose, boolean existing, boolean eligible) {
        UUID account = existing ? UUID.randomUUID() : null;
        if (existing) { jdbc.update("INSERT INTO accounts (id,email,session_version) VALUES (?,?,7)", account, EMAIL); }
        var response = issue(purpose);
        var row = jdbc.queryForMap("SELECT * FROM email_verifications WHERE id = ?", response.challengeId());
        assertThat(row).containsEntry("email", EMAIL).containsEntry("eligible", eligible);
        assertThat(response.expiresAt().getOffset().getTotalSeconds()).isEqualTo(32400);
        assertThat(row).containsEntry("reset_account_id", purpose == Purpose.RESET_PASSWORD && eligible ? account : null)
                .containsEntry("reset_session_version", purpose == Purpose.RESET_PASSWORD && eligible ? 7L : null);
        var capture = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(EMAIL), capture.capture(), eq(eligible));
        assertThat(capture.getValue()).matches("[0-9]{6}");
        assertThat((byte[]) row.get("code_hash")).containsExactly(keys.codeHasher().hash(capture.getValue()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE attempt_count = 1", Long.class)).isEqualTo(3);
    }

    @Test void actualIssueSpacingIsRecheckedAfterReservationWindowExpired() {
        var first = issue(Purpose.SIGNUP);
        expireCooldown();
        assertThatThrownBy(() -> issue(Purpose.SIGNUP)).isInstanceOf(AuthRateLimitException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_verifications", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT invalidated_at IS NULL FROM email_verifications WHERE id=?", Boolean.class, first.challengeId())).isTrue();
        verify(sender, times(1)).send(anyString(), anyString(), anyBoolean());
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action='EMAIL_SEND_SUBJECT'", Long.class)).isEqualTo(2);
    }

    @Test void allowedReissueInvalidatesVerifiedUnconsumedToken() {
        var first = issue(Purpose.SIGNUP);
        jdbc.update("""
                UPDATE email_verifications SET created_at=clock_timestamp()-INTERVAL '61 seconds',
                    verified_at=clock_timestamp()-INTERVAL '1 second',token_hash=?,
                    token_expires_at=clock_timestamp()+INTERVAL '10 minutes' WHERE id=?
                """, new byte[32], first.challengeId());
        expireCooldown();
        var second = issue(Purpose.SIGNUP);
        assertThat(jdbc.queryForObject("SELECT invalidated_at IS NOT NULL FROM email_verifications WHERE id=?", Boolean.class, first.challengeId())).isTrue();
        assertThat(jdbc.queryForObject("SELECT invalidated_at IS NULL FROM email_verifications WHERE id=?", Boolean.class, second.challengeId())).isTrue();
    }

    @Test void failedMailInvalidatesChallengeButKeepsReservedBudgets() {
        doThrow(new RuntimeException("synthetic mail detail")).when(sender).send(anyString(), anyString(), anyBoolean());
        assertThatThrownBy(() -> issue(Purpose.SIGNUP)).isInstanceOf(EmailVerificationException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM email_verifications WHERE invalidated_at IS NOT NULL", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE attempt_count=1", Long.class)).isEqualTo(3);
    }

    @Test void accountReadFailureIsUnavailableAndKeepsReservations() {
        jdbc.execute("ALTER TABLE accounts RENAME TO unavailable_accounts");
        try {
            assertThatThrownBy(() -> issue(Purpose.SIGNUP)).isInstanceOf(AuthStateUnavailableException.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE attempt_count=1", Long.class)).isEqualTo(3);
        }
        finally { jdbc.execute("ALTER TABLE unavailable_accounts RENAME TO accounts"); }
    }

    private com.dodamdodam.backend.auth.dto.VerificationIssueResponse issue(Purpose purpose) {
        return issuance.issue(new MockHttpServletRequest(), new IssueEmailVerificationRequest(purpose, EMAIL), null,
                InetAddress.ofLiteral("192.0.2.10"));
    }
    private void expireCooldown() {
        jdbc.update("UPDATE auth_rate_limits SET window_started_at=clock_timestamp()-INTERVAL '61 seconds', expires_at=clock_timestamp()-INTERVAL '1 second' WHERE action='EMAIL_RESEND'");
    }
}
