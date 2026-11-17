package com.dodamdodam.backend.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordLoginRepositoryTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordLoginRepository repository;

    @BeforeEach
    @AfterEach
    void cleanRows() {
        jdbc.update("DELETE FROM accounts");
    }

    @Test
    void readsOnlyMatchingCredentialAndVersionWithoutLoggingHash() {
        var account = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts(id,email,password_hash,session_version) VALUES(?,?,?,7)",
                account, "u@example.invalid", "synthetic-stored-hash");
        jdbc.update("INSERT INTO accounts(id,email,password_hash) VALUES(?,?,?)",
                UUID.randomUUID(), "other@example.invalid", "synthetic-other-hash");

        var credential = repository.findCredential("u@example.invalid").orElseThrow();
        assertThat(credential.accountId()).isEqualTo(account);
        assertThat(credential.passwordHash()).isEqualTo("synthetic-stored-hash");
        assertThat(credential.sessionVersion()).isEqualTo(7);
        assertThat(credential.toString()).doesNotContain("synthetic", account.toString());
        assertThat(repository.findCredential("missing@example.invalid")).isEmpty();
    }

    @Test
    void socialOnlyAccountHasNoPasswordCredential() {
        jdbc.update("INSERT INTO accounts(id,email) VALUES(?,?)", UUID.randomUUID(), "social@example.invalid");
        assertThat(repository.findCredential("social@example.invalid").orElseThrow().passwordHash()).isNull();
    }

    @Test
    void missingTableIs503RatherThanMissingAccount() {
        jdbc.execute("ALTER TABLE accounts RENAME TO unavailable_login_accounts");
        try {
            assertThatThrownBy(() -> repository.findCredential("u@example.invalid"))
                    .isInstanceOf(AuthStateUnavailableException.class);
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_login_accounts RENAME TO accounts");
        }
    }
}
