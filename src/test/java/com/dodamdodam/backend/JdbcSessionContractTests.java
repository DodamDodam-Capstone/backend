package com.dodamdodam.backend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "logging.level.org.springframework.session=INFO", "spring.session.jdbc.cleanup-cron=-"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JdbcSessionContractTests {

    @Autowired
    private JdbcIndexedSessionRepository sessionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearSessions() {
        jdbcTemplate.update("DELETE FROM SPRING_SESSION");
    }

    @Test
    void savedSessionCanBeLoadedByAnotherRepository() {
        var session = savedSession(sessionRepository,
                value -> value.setAttribute("contract-marker", "persisted"));
        var transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        var secondRepository = new JdbcIndexedSessionRepository(jdbcTemplate, transaction);

        Session reloaded = secondRepository.findById(session.getId());

        assertThat(reloaded != null).as("another repository loads the stored session").isTrue();
        assertThat(reloaded.<String>getAttribute("contract-marker")).isEqualTo("persisted");
    }

    @Test
    void changedAttributeIsStoredBeforeResponseCommit() {
        Session session = sessionRepository.createSession();
        session.setAttribute("contract-marker", "immediate");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES WHERE ATTRIBUTE_NAME = ?",
                Integer.class, "contract-marker")).isEqualTo(1);
    }

    @Test
    void anonymousSessionExpires() {
        var past = Instant.now().minusSeconds(3600);
        var session = savedSession(sessionRepository, value -> value.setLastAccessedTime(past));

        assertThat(session.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(1800));
        assertThat(sessionRepository.findById(session.getId()) == null)
                .as("anonymous session expires after the finite timeout").isTrue();
    }

    @Test
    void unlimitedSessionSurvivesCleanup() {
        var past = Instant.parse("2000-01-01T00:00:00Z");
        var session = savedSession(sessionRepository, value -> {
            value.setMaxInactiveInterval(Duration.ofSeconds(-1));
            value.setLastAccessedTime(past);
            value.setAttribute("contract-marker", "unlimited");
        });

        sessionRepository.cleanUpExpiredSessions();

        Session reloaded = sessionRepository.findById(session.getId());
        assertThat(reloaded != null).as("unlimited session survives cleanup").isTrue();
        assertThat(reloaded.getMaxInactiveInterval()).isEqualTo(Duration.ofSeconds(-1));
        Long storedExpiry = jdbcTemplate.queryForObject(
                "SELECT EXPIRY_TIME FROM SPRING_SESSION WHERE SESSION_ID = ?",
                Long.class, session.getId());
        assertThat(storedExpiry).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void cleanupRemovesExpiredSessionAttributes() {
        var past = Instant.now().minusSeconds(3600);
        savedSession(sessionRepository, value -> {
            value.setLastAccessedTime(past);
            value.setAttribute("contract-marker", "expired");
        });

        sessionRepository.cleanUpExpiredSessions();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(longs = {1800, -1})
    void explicitDeletionRemovesStoredSessionAndAttributes(long inactiveSeconds) {
        var session = savedSession(sessionRepository, value -> {
            value.setMaxInactiveInterval(Duration.ofSeconds(inactiveSeconds));
            value.setAttribute("contract-marker", "deleted");
        });

        sessionRepository.deleteById(session.getId());

        assertThat(sessionRepository.findById(session.getId()) == null)
                .as("deleted session cannot be loaded").isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES", Integer.class)).isZero();
    }

    private static <S extends Session> Session savedSession(
            SessionRepository<S> repository, Consumer<Session> configure) {
        S session = repository.createSession();
        configure.accept(session);
        repository.save(session);
        return session;
    }
}
