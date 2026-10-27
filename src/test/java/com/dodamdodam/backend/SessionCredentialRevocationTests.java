package com.dodamdodam.backend;

import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SessionCredentialRevocationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SessionSecurityRepository sessionSecurityRepository;

    @BeforeEach
    void clearCredentials() {
        jdbcTemplate.update("DELETE FROM email_verifications");
        jdbcTemplate.update("DELETE FROM session_security");
        jdbcTemplate.update("DELETE FROM accounts");
    }

    @Test
    void ordinaryContextAllowsNullExpiryAndUsesVersionDefaults() {
        var accountId = account();
        var contextId = context(accountId);

        var stored = jdbcTemplate.queryForMap(
                "SELECT expires_at, session_version, setup_generation FROM session_security WHERE id = ?",
                contextId);
        assertThat(stored.get("expires_at")).isNull();
        assertThat(stored.get("session_version")).isEqualTo(0L);
        assertThat(stored.get("setup_generation")).isEqualTo(0L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT session_version FROM accounts WHERE id = ?", Long.class, accountId)).isZero();
    }

    @Test
    void securityContextRequiresAnExistingAccount() {
        assertThatThrownBy(() -> context(UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void accountVersionCannotBeNegative() {
        var accountId = account();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE accounts SET session_version = -1 WHERE id = ?", accountId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"session_version", "setup_generation"})
    void contextVersionsCannotBeNegative(String column) {
        var contextId = context(account());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE session_security SET " + column + " = -1 WHERE id = ?", contextId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"guardian_unlocked_until = CURRENT_TIMESTAMP", "guardian_pin_version = 1"})
    void guardianAuthorityRequiresBothFields(String assignment) {
        var contextId = context(account());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE session_security SET " + assignment + " WHERE id = ?", contextId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void finiteExpiryMustBeAfterContextCreation() {
        var contextId = context(account());
        jdbcTemplate.update("""
                UPDATE session_security SET expires_at = CURRENT_TIMESTAMP + INTERVAL '30 minutes'
                WHERE id = ?
                """, contextId);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE session_security SET expires_at = created_at WHERE id = ?", contextId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIN_SETUP", "PIN_RESET"})
    void pinProofCannotBindAnotherAccountsContext(String purpose) {
        var firstAccount = account();
        var secondContext = context(account());

        assertThatThrownBy(() -> pinChallenge(firstAccount, secondContext, purpose))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void consumedProofCannotAlsoBeInvalidated() {
        var accountId = account();
        var proofId = verifiedPinProof(accountId, context(accountId), "PIN_SETUP");
        jdbcTemplate.update("""
                UPDATE email_verifications SET token_consumed_at = CURRENT_TIMESTAMP WHERE id = ?
                """, proofId);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                UPDATE email_verifications SET invalidated_at = CURRENT_TIMESTAMP WHERE id = ?
                """, proofId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revocationClearsOnlyCurrentUnconsumedPinCredentials() {
        var accountId = account();
        var contextId = context(accountId);
        var otherContext = context(accountId);
        var otherAccount = account();
        var otherAccountContext = context(otherAccount);
        grantGuardian(contextId);
        grantGuardian(otherContext);
        jdbcTemplate.update("UPDATE session_security SET setup_generation = 3 WHERE id = ?", contextId);
        var otherContextBefore = contextState(otherContext);
        var otherAccountContextBefore = contextState(otherAccountContext);
        var revoke = new ArrayList<UUID>();
        var preserve = new ArrayList<UUID>();
        for (var purpose : List.of("PIN_SETUP", "PIN_RESET")) {
            revoke.add(pinChallenge(accountId, contextId, purpose));
            var currentProof = verifiedPinProof(accountId, contextId, purpose);
            jdbcTemplate.update("UPDATE email_verifications SET setup_generation = 3 WHERE id = ?", currentProof);
            revoke.add(currentProof);
            var consumed = verifiedPinProof(accountId, contextId, purpose);
            jdbcTemplate.update("""
                    UPDATE email_verifications SET token_consumed_at = CURRENT_TIMESTAMP WHERE id = ?
                    """, consumed);
            preserve.add(consumed);
            var invalidated = pinChallenge(accountId, contextId, purpose);
            jdbcTemplate.update("""
                    UPDATE email_verifications SET invalidated_at = CURRENT_TIMESTAMP WHERE id = ?
                    """, invalidated);
            preserve.add(invalidated);
            preserve.add(verifiedPinProof(accountId, otherContext, purpose));
            preserve.add(verifiedPinProof(otherAccount, otherAccountContext, purpose));
        }
        preserve.add(publicChallenge(accountId, "SIGNUP"));
        preserve.add(publicChallenge(accountId, "RESET_PASSWORD"));
        var preservedStates = preserve.stream().map(this::proofState).toList();

        assertThat(sessionSecurityRepository.revokeCurrentContext(accountId, contextId)).isTrue();

        var stored = contextState(contextId);
        assertThat(stored.get("revoked_at")).isNotNull();
        assertThat(stored.get("guardian_unlocked_until")).isNull();
        assertThat(stored.get("guardian_pin_version")).isNull();
        assertThat(stored.get("session_version")).isEqualTo(0L);
        assertThat(stored.get("setup_generation")).isEqualTo(3L);
        assertThat(stored.get("expires_at")).isNull();
        for (var proof : revoke) {
            assertThat(proofState(proof).get("invalidated_at")).isEqualTo(stored.get("revoked_at"));
        }
        assertThat(preserve.stream().map(this::proofState).toList()).isEqualTo(preservedStates);
        assertThat(contextState(otherContext)).isEqualTo(otherContextBefore);
        assertThat(contextState(otherAccountContext)).isEqualTo(otherAccountContextBefore);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT session_version FROM accounts WHERE id = ?", Long.class, accountId)).isZero();
    }

    @Test
    void contextWithoutPinProofsCanStillBeRevoked() {
        var accountId = account();
        var contextId = context(accountId);
        grantGuardian(contextId);

        assertThat(sessionSecurityRepository.revokeCurrentContext(accountId, contextId)).isTrue();

        var stored = contextState(contextId);
        assertThat(stored.get("revoked_at")).isNotNull();
        assertThat(stored.get("guardian_unlocked_until")).isNull();
        assertThat(stored.get("guardian_pin_version")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING_ACCOUNT", "MISSING_CONTEXT", "OTHER_ACCOUNT", "STALE_VERSION",
            "REVOKED", "EXPIRED"})
    void invalidBindingCannotChangeCredentials(String invalidBinding) {
        var accountId = account();
        var contextId = context(accountId);
        var proof = verifiedPinProof(accountId, contextId, "PIN_RESET");
        var requestedAccount = accountId;
        var requestedContext = contextId;
        switch (invalidBinding) {
            case "MISSING_ACCOUNT" -> requestedAccount = UUID.randomUUID();
            case "MISSING_CONTEXT" -> requestedContext = UUID.randomUUID();
            case "OTHER_ACCOUNT" -> requestedAccount = account();
            case "STALE_VERSION" -> jdbcTemplate.update(
                    "UPDATE accounts SET session_version = 1 WHERE id = ?", accountId);
            case "REVOKED" -> jdbcTemplate.update(
                    "UPDATE session_security SET revoked_at = CURRENT_TIMESTAMP WHERE id = ?", contextId);
            case "EXPIRED" -> jdbcTemplate.update("""
                    UPDATE session_security SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                    WHERE id = ?
                    """, contextId);
            default -> throw new AssertionError("Unknown test fixture");
        }
        var before = contextState(contextId);
        var proofBefore = proofState(proof);

        assertThat(sessionSecurityRepository.revokeCurrentContext(requestedAccount, requestedContext)).isFalse();

        assertThat(contextState(contextId)).isEqualTo(before);
        assertThat(proofState(proof)).isEqualTo(proofBefore);
    }

    @Test
    void repeatedRevocationDoesNotRewriteItsTimestamp() {
        var accountId = account();
        var contextId = context(accountId);
        var proof = verifiedPinProof(accountId, contextId, "PIN_SETUP");
        assertThat(sessionSecurityRepository.revokeCurrentContext(accountId, contextId)).isTrue();
        var before = contextState(contextId);
        var proofBefore = proofState(proof);

        assertThat(sessionSecurityRepository.revokeCurrentContext(accountId, contextId)).isFalse();

        assertThat(contextState(contextId)).isEqualTo(before);
        assertThat(proofState(proof)).isEqualTo(proofBefore);
    }

    @Test
    void proofWriteFailureRollsBackContextAndGuardianRevocation() {
        var accountId = account();
        var contextId = context(accountId);
        grantGuardian(contextId);
        var setupProof = verifiedPinProof(accountId, contextId, "PIN_SETUP");
        var resetProof = verifiedPinProof(accountId, contextId, "PIN_RESET");
        var before = contextState(contextId);
        var proofsBefore = List.of(proofState(setupProof), proofState(resetProof));
        jdbcTemplate.execute("""
                ALTER TABLE email_verifications ADD CONSTRAINT test_revoke_failure CHECK (invalidated_at IS NULL)
                """);
        try {
            assertThatThrownBy(() -> sessionSecurityRepository.revokeCurrentContext(accountId, contextId))
                    .isInstanceOf(DataIntegrityViolationException.class);

            assertThat(contextState(contextId)).isEqualTo(before);
            assertThat(List.of(proofState(setupProof), proofState(resetProof))).isEqualTo(proofsBefore);
        }
        finally {
            jdbcTemplate.execute("ALTER TABLE email_verifications DROP CONSTRAINT test_revoke_failure");
        }
    }

    private Map<String, Object> contextState(UUID contextId) {
        return jdbcTemplate.queryForMap("""
                SELECT session_version, setup_generation, guardian_unlocked_until, guardian_pin_version,
                       expires_at, revoked_at
                FROM session_security WHERE id = ?
                """, contextId);
    }

    private Map<String, Object> proofState(UUID proofId) {
        return jdbcTemplate.queryForMap("""
                SELECT invalidated_at, verified_at, token_expires_at, token_consumed_at,
                       setup_generation, pin_version_snapshot
                FROM email_verifications WHERE id = ?
                """, proofId);
    }

    private void grantGuardian(UUID contextId) {
        jdbcTemplate.update("""
                UPDATE session_security
                SET guardian_unlocked_until = CURRENT_TIMESTAMP + INTERVAL '10 minutes', guardian_pin_version = 1
                WHERE id = ?
                """, contextId);
    }

    private UUID account() {
        var id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO accounts (id, email) VALUES (?, ?)", id, id + "@example.invalid");
        return id;
    }

    private UUID context(UUID accountId) {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO session_security (id, account_id, session_version, created_at)
                VALUES (?, ?, 0, CURRENT_TIMESTAMP - INTERVAL '1 minute')
                """, id, accountId);
        return id;
    }

    private UUID pinChallenge(UUID accountId, UUID contextId, String purpose) {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO email_verifications
                    (id, email, purpose, eligible, code_hash, expires_at, setup_account_id,
                     setup_session_binding, setup_generation, pin_version_snapshot, created_at)
                VALUES (?, ?, ?, true, ?, CURRENT_TIMESTAMP + INTERVAL '10 minutes', ?, ?, 0, ?,
                        CURRENT_TIMESTAMP - INTERVAL '1 minute')
                """, id, accountId + "@example.invalid", purpose, new byte[32], accountId, contextId,
                purpose.equals("PIN_RESET") ? 1L : null);
        return id;
    }

    private UUID verifiedPinProof(UUID accountId, UUID contextId, String purpose) {
        var id = pinChallenge(accountId, contextId, purpose);
        jdbcTemplate.update("""
                UPDATE email_verifications
                SET verified_at = CURRENT_TIMESTAMP - INTERVAL '1 second', token_hash = ?,
                    token_expires_at = CURRENT_TIMESTAMP + INTERVAL '10 minutes'
                WHERE id = ?
                """, new byte[32], id);
        return id;
    }

    private UUID publicChallenge(UUID accountId, String purpose) {
        var id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO email_verifications
                    (id, email, purpose, eligible, code_hash, expires_at,
                     reset_account_id, reset_session_version, created_at)
                VALUES (?, ?, ?, true, ?, CURRENT_TIMESTAMP + INTERVAL '10 minutes', ?, ?,
                        CURRENT_TIMESTAMP - INTERVAL '1 minute')
                """, id, accountId + "@example.invalid", purpose, new byte[32],
                purpose.equals("RESET_PASSWORD") ? accountId : null,
                purpose.equals("RESET_PASSWORD") ? 0L : null);
        return id;
    }
}
