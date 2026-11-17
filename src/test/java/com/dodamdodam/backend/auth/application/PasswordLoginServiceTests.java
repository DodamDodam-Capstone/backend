package com.dodamdodam.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.dto.LoginRequest;
import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.support.PasswordLoginTestConfiguration;
import com.dodamdodam.backend.auth.support.PasswordLoginTestConfiguration.ObservedPasswords;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;

@Import({TestcontainersConfiguration.class, PasswordLoginTestConfiguration.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordLoginServiceTests {
    @Autowired private PasswordLoginService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObservedPasswords passwords;

    @BeforeEach
    @AfterEach
    void cleanRows() {
        passwords.reset();
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM auth_rate_limits");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "SOCIAL", "WRONG"})
    void allCredentialFailuresPerformOneRealHashComparisonAndReturnSameError(String type) {
        if (!type.equals("MISSING")) {
            jdbc.update("INSERT INTO accounts(id,email,password_hash) VALUES(?,?,?)", UUID.randomUUID(),
                    "u@example.invalid", type.equals("SOCIAL") ? null : passwords.encode("synthetic-correct-password"));
        }
        assertThatThrownBy(() -> login())
                .isInstanceOfSatisfying(PasswordLoginException.class, e -> {
                    assertThat(e.status()).isEqualTo(401);
                    assertThat(e.code()).isEqualTo("LOGIN_FAILED");
                    assertThat(e.fields()).isEmpty();
                    assertThat(e.getMessage()).doesNotContain("example.invalid", "synthetic");
                });
        assertThat(passwords.comparisons).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM session_security", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT sum(attempt_count) FROM auth_rate_limits", Long.class)).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{unknown}synthetic", "{pbkdf2-sha256-v1}not-hex", "{pbkdf2-sha256-v1}00"})
    void malformedStoredHashIs503AndCannotCreateSession(String hash) {
        jdbc.update("INSERT INTO accounts(id,email,password_hash) VALUES(?,?,?)", UUID.randomUUID(), "u@example.invalid", hash);
        assertThatThrownBy(this::login).isInstanceOf(AuthStateUnavailableException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM SPRING_SESSION", Long.class)).isZero();
    }

    @Test
    void lookupOutageIs503RatherThanMissingAccount() {
        jdbc.execute("ALTER TABLE accounts RENAME TO unavailable_password_lookup");
        try {
            assertThatThrownBy(this::login).isInstanceOf(AuthStateUnavailableException.class);
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_password_lookup RENAME TO accounts");
        }
        assertThat(passwords.comparisons).hasValue(0);
    }

    /** 구조 검증을 통과한 원문으로 실제 예산·조회·encoder 경로를 호출한다. */
    private void login() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        service.login(new LoginRequest("u@example.invalid", "synthetic-wrong-password"), request, new MockHttpServletResponse());
    }
}
