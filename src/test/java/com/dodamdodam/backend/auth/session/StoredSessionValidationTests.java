package com.dodamdodam.backend.auth.session;

import com.dodamdodam.backend.TestcontainersConfiguration;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 요청 객체와 분리된 실제 JDBC 저장 상태로 공통 재검증 계약을 확인한다. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.session.jdbc.cleanup-cron=-")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StoredSessionValidationTests {

    @Autowired private AuthenticatedSessionService authentication;
    @Autowired private JdbcIndexedSessionRepository sessions;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM session_security");
        jdbc.update("DELETE FROM accounts");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 1800})
    void returnsTheReloadedValidSessionWithoutChangingItsLifetime(long inactiveSeconds) {
        var fixture = authenticated(inactiveSeconds);

        var stored = authentication.requireCurrentStoredSession(fixture.session().getId(), fixture.context());

        assertThat(stored).isNotSameAs(fixture.session());
        assertThat(stored.getId()).isEqualTo(fixture.session().getId());
        assertThat(stored.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(inactiveSeconds));
        assertThat(stored.<UUID>getAttribute(AuthenticatedSessionService.ACCOUNT_ID))
                .isEqualTo(fixture.context().accountId());
        assertThat(stored.<UUID>getAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID))
                .isEqualTo(fixture.context().securityContextId());
    }

    @Test
    void rejectsDeletedSessionEvenWhileTheOriginalObjectAndBusinessContextRemainValid() {
        var fixture = authenticated(-1);
        sessions.deleteById(fixture.session().getId());

        assertInvalid(fixture);
    }

    @Test
    void rejectsExpiredStoredSession() {
        var fixture = authenticated(60);
        updateStoredSession(sessions, fixture.session().getId(), session ->
                session.setLastAccessedTime(Instant.now().minusSeconds(120)));

        assertInvalid(fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCOUNT", "CONTEXT", "PRINCIPAL", "MISSING_SECURITY"})
    void rejectsStoredLoginContextThatDiffersFromTheExpectedContext(String changed) {
        var fixture = authenticated(-1);
        // 별도 조회 객체에 저장하여 최초 객체가 아닌 JDBC 상태를 검사하는지 확인한다.
        updateStoredSession(sessions, fixture.session().getId(), changedSession -> {
            switch (changed) {
                case "ACCOUNT" -> {
                    var other = authenticated(-1);
                    changedSession.setAttribute(AuthenticatedSessionService.ACCOUNT_ID, other.context().accountId());
                    changedSession.setAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID, other.context().securityContextId());
                    changedSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            other.session().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
                }
                case "CONTEXT" -> {
                    var otherContext = UUID.randomUUID();
                    jdbc.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 0)",
                            otherContext, fixture.context().accountId());
                    changedSession.setAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID, otherContext);
                }
                case "PRINCIPAL" -> changedSession.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        securityContext(UUID.randomUUID()));
                case "MISSING_SECURITY" -> changedSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                default -> throw new AssertionError("Unknown fixture");
            }
        });

        assertInvalid(fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "REVOKED", "EXPIRED", "VERSION"})
    void rejectsInvalidBusinessSession(String invalid) {
        var fixture = authenticated(-1);
        switch (invalid) {
            case "MISSING" -> jdbc.update("DELETE FROM session_security WHERE id = ?", fixture.context().securityContextId());
            case "REVOKED" -> jdbc.update("UPDATE session_security SET revoked_at = clock_timestamp() WHERE id = ?",
                    fixture.context().securityContextId());
            case "EXPIRED" -> jdbc.update("UPDATE session_security SET expires_at = clock_timestamp() WHERE id = ?",
                    fixture.context().securityContextId());
            case "VERSION" -> jdbc.update("UPDATE accounts SET session_version = 1 WHERE id = ?", fixture.context().accountId());
            default -> throw new AssertionError("Unknown fixture");
        }

        assertInvalid(fixture);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SPRING_SESSION", "SPRING_SESSION_ATTRIBUTES", "session_security"})
    void distinguishesJdbcLookupFailureFromInvalidAuthentication(String table) {
        var fixture = authenticated(-1);
        // 테스트 전용 DB에서만 장애를 주입하고 실패해도 원래 테이블을 복원한다.
        jdbc.execute("ALTER TABLE " + table + " RENAME TO unavailable_stored_session");
        try {
            assertThatThrownBy(() -> authentication.requireCurrentStoredSession(fixture.session().getId(), fixture.context()))
                    .isInstanceOf(AuthStateUnavailableException.class)
                    .hasCauseInstanceOf(org.springframework.dao.DataAccessException.class);
        }
        finally {
            jdbc.execute("ALTER TABLE unavailable_stored_session RENAME TO " + table);
        }
    }

    private void assertInvalid(Fixture fixture) {
        assertThatThrownBy(() -> authentication.requireCurrentStoredSession(fixture.session().getId(), fixture.context()))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    /** 유효한 업무 문맥과 JDBC 세션을 만들고 원본 객체를 유지한다. */
    private Fixture authenticated(long inactiveSeconds) {
        var context = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", context.accountId(), context.accountId() + "@example.invalid");
        jdbc.update("INSERT INTO session_security (id, account_id, session_version) VALUES (?, ?, 0)",
                context.securityContextId(), context.accountId());
        Session session = sessions.createSession();
        session.setMaxInactiveInterval(Duration.ofSeconds(inactiveSeconds));
        session.setAttribute(AuthenticatedSessionService.ACCOUNT_ID, context.accountId());
        session.setAttribute(AuthenticatedSessionService.SECURITY_CONTEXT_ID, context.securityContextId());
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext(context.accountId()));
        return new Fixture(session, context);
    }

    /** 비공개 JDBC 구현 타입을 참조하지 않고 재조회한 세션의 변경을 저장한다. */
    private static <S extends Session> void updateStoredSession(
            SessionRepository<S> repository, String sessionId, Consumer<Session> update) {
        S session = repository.findById(sessionId);
        update.accept(session);
        repository.save(session);
    }

    private SecurityContext securityContext(UUID accountId) {
        var security = SecurityContextHolder.createEmptyContext();
        security.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(accountId.toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        return security;
    }

    private record Fixture(Session session, AuthenticatedSession context) { }
}
