package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRateLimitKeyHasherTests {

    private static final InetAddress IP = InetAddress.ofLiteral("192.0.2.10");

    @Test
    void stableHmacVectorPreservesExistingBudgetKeysAcrossInstances() {
        var key = new byte[32];
        for (int index = 0; index < key.length; index++) {
            key[index] = (byte) index;
        }
        var hasher = new AuthRateLimitKeyHasher(key);

        var actual = find(hasher.emailIssueKeys("person@example.invalid", Purpose.SIGNUP, null, null, IP),
                Action.EMAIL_SEND_SUBJECT);

        assertThat(HexFormat.of().formatHex(actual.keyHash()))
                .isEqualTo("4201e5498a0daa9a3f99df411800c288891214603d5274961b0e05e60f941965");
        assertThat(find(new AuthRateLimitKeyHasher(key).emailIssueKeys(
                "person@example.invalid", Purpose.SIGNUP, null, null, IP), Action.EMAIL_SEND_SUBJECT).keyHash())
                .containsExactly(actual.keyHash());
    }

    @Test
    void publicKeysSeparatePurposeAndEmailButShareIpBudget() {
        var hasher = new AuthRateLimitKeyHasher(new byte[32]);
        var signup = hasher.emailIssueKeys("person@example.invalid", Purpose.SIGNUP, null, null, IP);
        var reset = hasher.emailIssueKeys("person@example.invalid", Purpose.RESET_PASSWORD, null, null, IP);
        var other = hasher.emailIssueKeys("other@example.invalid", Purpose.SIGNUP, null, null, IP);
        assertThat(find(signup, Action.EMAIL_SEND_SUBJECT).keyHash())
                .isNotEqualTo(find(reset, Action.EMAIL_SEND_SUBJECT).keyHash())
                .isNotEqualTo(find(other, Action.EMAIL_SEND_SUBJECT).keyHash())
                .isNotEqualTo(find(signup, Action.EMAIL_RESEND).keyHash());
        assertThat(find(signup, Action.EMAIL_SEND_IP).keyHash())
                .containsExactly(find(reset, Action.EMAIL_SEND_IP).keyHash())
                .containsExactly(find(other, Action.EMAIL_SEND_IP).keyHash())
                .isNotEqualTo(hasher.apiIp(IP).keyHash());
    }

    @Test
    void pinSubjectSurvivesSessionAndEmailChangesWhileCooldownUsesCurrentContext() {
        var hasher = new AuthRateLimitKeyHasher(new byte[32]);
        var accountId = UUID.randomUUID();
        var contextId = UUID.randomUUID();
        var first = hasher.emailIssueKeys("old@example.invalid", Purpose.PIN_SETUP, accountId, contextId, IP);
        var changedEmail = hasher.emailIssueKeys("new@example.invalid", Purpose.PIN_SETUP, accountId, contextId, IP);
        var otherSession = hasher.emailIssueKeys(null, Purpose.PIN_SETUP, accountId, UUID.randomUUID(), IP);
        var reset = hasher.emailIssueKeys(null, Purpose.PIN_RESET, accountId, contextId, IP);
        var otherAccount = hasher.emailIssueKeys(null, Purpose.PIN_SETUP, UUID.randomUUID(), UUID.randomUUID(), IP);

        assertThat(find(first, Action.EMAIL_SEND_SUBJECT).keyHash())
                .containsExactly(find(changedEmail, Action.EMAIL_SEND_SUBJECT).keyHash())
                .containsExactly(find(otherSession, Action.EMAIL_SEND_SUBJECT).keyHash())
                .isNotEqualTo(find(reset, Action.EMAIL_SEND_SUBJECT).keyHash())
                .isNotEqualTo(find(otherAccount, Action.EMAIL_SEND_SUBJECT).keyHash());
        assertThat(find(first, Action.EMAIL_RESEND).keyHash())
                .containsExactly(find(changedEmail, Action.EMAIL_RESEND).keyHash())
                .isNotEqualTo(find(otherSession, Action.EMAIL_RESEND).keyHash());
    }

    @Test
    void ipKeysUseAddressBytesRatherThanTextSpelling() {
        var hasher = new AuthRateLimitKeyHasher(new byte[32]);
        assertThat(hasher.apiIp(InetAddress.ofLiteral("::1")).keyHash())
                .containsExactly(hasher.apiIp(InetAddress.ofLiteral("0:0:0:0:0:0:0:1")).keyHash())
                .isNotEqualTo(hasher.apiIp(IP).keyHash());
    }

    @Test
    void callerCannotMutateKeyMaterialOrStoredHash() {
        var secret = new byte[32];
        var hasher = new AuthRateLimitKeyHasher(secret);
        var first = hasher.apiIp(IP);
        var originalHash = first.keyHash();
        Arrays.fill(secret, (byte) 1);
        Arrays.fill(first.keyHash(), (byte) 2);
        var suppliedHash = originalHash.clone();
        var transported = new AuthRateLimitKey(Action.API_IP, suppliedHash);
        Arrays.fill(suppliedHash, (byte) 3);
        assertThat(hasher.apiIp(IP).keyHash()).containsExactly(originalHash);
        assertThat(first.keyHash()).containsExactly(originalHash);
        assertThat(transported.keyHash()).containsExactly(originalHash);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 31, 33, 64})
    void missingOrInvalidSecretFailsClosed(int size) {
        assertThatThrownBy(() -> new AuthRateLimitKeyHasher(size < 0 ? null : new byte[size]))
                .isInstanceOf(AuthStateUnavailableException.class);
    }

    @Test
    void missingScopeDoesNotFallBackToSharedOrUnlimitedBudget() {
        var hasher = new AuthRateLimitKeyHasher(new byte[32]);
        assertThatThrownBy(() -> hasher.apiIp(null)).isInstanceOf(AuthStateUnavailableException.class);
        assertThatThrownBy(() -> hasher.emailIssueKeys(null, Purpose.SIGNUP, null, null, IP))
                .isInstanceOf(AuthStateUnavailableException.class);
        assertThatThrownBy(() -> hasher.emailIssueKeys(null, Purpose.PIN_SETUP, null, UUID.randomUUID(), IP))
                .isInstanceOf(AuthStateUnavailableException.class);
        assertThatThrownBy(() -> hasher.emailIssueKeys(null, Purpose.PIN_RESET, UUID.randomUUID(), null, IP))
                .isInstanceOf(AuthStateUnavailableException.class);
    }

    private AuthRateLimitKey find(List<AuthRateLimitKey> keys, Action action) {
        return keys.stream().filter(key -> key.action() == action).findFirst().orElseThrow();
    }
}
