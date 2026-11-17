package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.infrastructure.*;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailVerificationDeliveryTests {
    @Autowired EmailVerificationRepository repository;
    @Autowired EmailVerificationDeliveryService delivery;
    @Autowired PlatformTransactionManager manager;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean SmtpEmailVerificationSender sender;

    @BeforeEach @AfterEach void clean() { jdbc.update("DELETE FROM email_verifications"); }

    @Test void sendsOutsideTransactionAndFailureInvalidatesOnlyThatIssue() {
        var first = save("first@example.invalid");
        var second = save("second@example.invalid");
        doAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw new RuntimeException("synthetic SMTP detail 000042 recipient@example.invalid");
        }).when(sender).send(anyString(), anyString(), anyBoolean());
        assertThatThrownBy(() -> delivery.deliver(first, "first@example.invalid", "000042", true))
                .isInstanceOfSatisfying(EmailVerificationException.class, error -> {
                    assertThat(error.code()).isEqualTo("EMAIL_DELIVERY_UNAVAILABLE");
                    assertThat(error.getMessage()).doesNotContain("000042", "example.invalid", "SMTP detail");
                    assertThat(error.getCause()).isNull();
                });
        assertThat(jdbc.queryForObject("SELECT invalidated_at IS NOT NULL FROM email_verifications WHERE id = ?", Boolean.class, first)).isTrue();
        assertThat(jdbc.queryForObject("SELECT invalidated_at IS NULL FROM email_verifications WHERE id = ?", Boolean.class, second)).isTrue();
    }

    @Test void compensationFailureIsAuthStateUnavailable() {
        var id = save("first@example.invalid");
        doAnswer(call -> { jdbc.execute("ALTER TABLE email_verifications RENAME TO unavailable_email_verifications");
            throw new RuntimeException("synthetic mail failure"); }).when(sender).send(anyString(), anyString(), anyBoolean());
        try {
            assertThatThrownBy(() -> delivery.deliver(id, "first@example.invalid", "000042", true))
                    .isInstanceOf(AuthStateUnavailableException.class);
        }
        finally { jdbc.execute("ALTER TABLE unavailable_email_verifications RENAME TO email_verifications"); }
    }

    private UUID save(String email) {
        var id = UUID.randomUUID();
        new TransactionTemplate(manager).executeWithoutResult(tx -> repository.replace(new EmailVerificationRepository.NewChallenge(
                id, email, Purpose.SIGNUP, true, new byte[32], null, null, null, null, null, null)));
        return id;
    }
}
