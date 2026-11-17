package com.dodamdodam.backend.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dodamdodam.backend.TestcontainersConfiguration;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.infrastructure.AuthRateLimitKeyHasher;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordLoginRateLimitTests {
    private static final InetAddress IP = InetAddress.ofLiteral("192.0.2.10");
    private final AuthRateLimitKeyHasher hasher = new AuthRateLimitKeyHasher(new byte[32]);
    @Autowired private AuthRateLimitService limits;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    @AfterEach
    void cleanRows() {
        jdbc.update("DELETE FROM auth_rate_limits");
    }

    @Test
    void sameEmailSharesBudgetAcrossIpsAndIpKeySeparatesAddressAndAction() {
        var first = hasher.loginKeys("u@example.invalid", IP);
        var otherIp = hasher.loginKeys("u@example.invalid", InetAddress.ofLiteral("192.0.2.11"));
        var otherEmail = hasher.loginKeys("other@example.invalid", IP);
        assertThat(find(first, Action.LOGIN_EMAIL).keyHash())
                .containsExactly(find(otherIp, Action.LOGIN_EMAIL).keyHash())
                .isNotEqualTo(find(otherEmail, Action.LOGIN_EMAIL).keyHash());
        assertThat(find(first, Action.LOGIN_IP).keyHash())
                .containsExactly(find(otherEmail, Action.LOGIN_IP).keyHash())
                .isNotEqualTo(find(otherIp, Action.LOGIN_IP).keyHash())
                .isNotEqualTo(hasher.apiIp(IP).keyHash());
    }

    @Test
    void tenthEmailIsAllowedAndRejectedEleventhDoesNotConsumeOtherIp() {
        var keys = hasher.loginKeys("u@example.invalid", IP);
        for (int attempt = 0; attempt < 10; attempt++) {
            limits.reserve(keys);
        }
        assertThatThrownBy(() -> limits.reserve(hasher.loginKeys("u@example.invalid",
                InetAddress.ofLiteral("192.0.2.11"))))
                .isInstanceOfSatisfying(AuthRateLimitException.class, e -> assertThat(e.retryAfterSeconds()).isBetween(1L, 900L));
        assertThat(count(find(keys, Action.LOGIN_EMAIL))).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action='LOGIN_IP'", Long.class)).isEqualTo(1);
    }

    @Test
    void hundredthIpIsAllowedAndRejectedEmailDoesNotConsumeAnySubjectBudget() {
        for (int attempt = 0; attempt < 100; attempt++) {
            limits.reserve(hasher.loginKeys("u" + attempt + "@example.invalid", IP));
        }
        assertThatThrownBy(() -> limits.reserve(hasher.loginKeys("new@example.invalid", IP)))
                .isInstanceOf(AuthRateLimitException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_rate_limits WHERE action='LOGIN_EMAIL'", Long.class)).isEqualTo(100);
        assertThat(count(find(hasher.loginKeys("unused@example.invalid", IP), Action.LOGIN_IP))).isEqualTo(100);
    }

    @Test
    void businessRollbackDoesNotRestoreReservedLoginBudgetAndWindowDoesNotSlide() {
        var keys = hasher.loginKeys("u@example.invalid", IP);
        limits.reserve(keys);
        var started = jdbc.queryForObject("SELECT window_started_at FROM auth_rate_limits WHERE action='LOGIN_EMAIL'",
                java.time.OffsetDateTime.class);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            limits.reserve(keys);
            status.setRollbackOnly();
        });
        assertThat(count(find(keys, Action.LOGIN_EMAIL))).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT window_started_at FROM auth_rate_limits WHERE action='LOGIN_EMAIL'",
                java.time.OffsetDateTime.class)).isEqualTo(started);
        jdbc.update("UPDATE auth_rate_limits SET window_started_at=clock_timestamp()-INTERVAL '901 seconds', expires_at=clock_timestamp()-INTERVAL '1 second', attempt_count=100");
        limits.reserve(keys);
        assertThat(count(find(keys, Action.LOGIN_EMAIL))).isEqualTo(1);
        assertThat(count(find(keys, Action.LOGIN_IP))).isEqualTo(1);
    }

    @Test
    void concurrentReservationsCannotExceedEmailOrIpBudget() throws Exception {
        var keys = hasher.loginKeys("u@example.invalid", IP);
        limits.reserve(keys);
        jdbc.update("UPDATE auth_rate_limits SET attempt_count=CASE WHEN action='LOGIN_EMAIL' THEN 9 ELSE 99 END");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = java.util.stream.IntStream.range(0, 20).mapToObj(i -> executor.submit(() -> {
                start.await();
                try {
                    limits.reserve(keys);
                    return true;
                }
                catch (AuthRateLimitException denied) {
                    return false;
                }
            })).toList();
            start.countDown();
            int successful = 0;
            for (var task : tasks) {
                if (task.get(15, TimeUnit.SECONDS)) {
                    successful++;
                }
            }
            assertThat(successful).isEqualTo(1);
        }
        assertThat(count(find(keys, Action.LOGIN_EMAIL))).isEqualTo(10);
        assertThat(count(find(keys, Action.LOGIN_IP))).isEqualTo(100);
    }

    /** DB에 남은 해당 예산만 읽어 실제 예약 효과를 확인한다. */
    private long count(AuthRateLimitKey key) {
        return jdbc.queryForObject("SELECT attempt_count FROM auth_rate_limits WHERE action=? AND key_hash=?",
                Long.class, key.action().name(), key.keyHash());
    }

    /** 키 묶음에서 검증할 업무 범위를 선택한다. */
    private AuthRateLimitKey find(List<AuthRateLimitKey> keys, Action action) {
        return keys.stream().filter(key -> key.action() == action).findFirst().orElseThrow();
    }
}
