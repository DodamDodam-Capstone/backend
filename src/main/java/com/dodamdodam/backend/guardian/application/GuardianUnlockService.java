package com.dodamdodam.backend.guardian.application;

import com.dodamdodam.backend.auth.application.AuthRateLimitService;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository;
import java.net.InetAddress;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GuardianUnlockService {
    private final AuthRateLimitService limits;
    private final AuthRateLimitRepository failures;
    private final EmailVerificationKeys keys;
    private final SessionSecurityRepository sessions;
    private final GuardianAccessRepository access;
    private final GuardianPinHasher pins;
    private final TransactionTemplate work;

    /** 예산 예약 뒤 PIN 확인과 오답 기록을 같은 업무 트랜잭션에서 처리한다. */
    public GuardianUnlockService(AuthRateLimitService limits, AuthRateLimitRepository failures,
                                 EmailVerificationKeys keys, SessionSecurityRepository sessions,
                                 GuardianAccessRepository access, GuardianPinHasher pins,
                                 PlatformTransactionManager transactionManager) {
        this.limits = limits;
        this.failures = failures;
        this.keys = keys;
        this.sessions = sessions;
        this.access = access;
        this.pins = pins;
        this.work = new TransactionTemplate(transactionManager);
        this.work.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 거절 응답보다 먼저 오답 기록을 확정해 다음 요청에 같은 이력을 적용한다. */
    public void unlock(AuthenticatedSession session, String pin, InetAddress address) {
        var rateHasher = keys.rateHasher();
        var budgets = rateHasher.pinUnlockKeys(session.accountId(), address);
        var failureKey = rateHasher.pinFailureKey(session.accountId());
        var allBudgets = new ArrayList<>(budgets);
        allBudgets.add(rateHasher.apiIp(address));
        try {
            try {
                limits.reserve(budgets);
            }
            catch (AuthRateLimitException limited) {
                throw retryAfter(allBudgets, failureKey, limited.retryAfterSeconds());
            }
            var decision = work.execute(status -> verifyInTransaction(session, pin, failureKey));
            if (decision == Decision.BLOCKED) {
                throw retryAfter(allBudgets, failureKey, 1);
            }
            if (decision == Decision.INCORRECT) {
                throw GuardianAccessException.incorrectPin();
            }
            if (decision == null) {
                throw new AuthStateUnavailableException();
            }
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 계정·문맥·PIN·오답 행을 잠근 다음 현재 차단과 PIN을 판정한다. */
    private Decision verifyInTransaction(AuthenticatedSession session, String pin, byte[] failureKey) {
        var account = session.accountId();
        var context = session.securityContextId();
        sessions.requireCurrentContextForUpdate(account, context);
        var stored = access.lockPinForUnlock(account).orElseThrow(EmailVerificationException::pinNotSet);
        var blockedUntil = failures.lockPinFailures(failureKey);
        if (blockedUntil != null && currentState(session).checkedAt().isBefore(blockedUntil)) {
            return Decision.BLOCKED;
        }

        boolean matched = pins.matches(pin, stored.hash());
        var state = currentState(session);
        if (!matched) {
            return failures.recordPinFailure(failureKey, state.checkedAt()) ? Decision.BLOCKED : Decision.INCORRECT;
        }
        var until = state.checkedAt().plusSeconds(1800);
        if (state.expiresAt() != null && state.expiresAt().isBefore(until)) {
            until = state.expiresAt();
        }
        // 공개한 초 경계와 저장소에서 거부하는 경계를 일치시킨다.
        until = until.truncatedTo(ChronoUnit.SECONDS);
        if (!state.checkedAt().isBefore(until)) {
            throw GuardianAccessException.authenticationRequired();
        }
        access.storeUnlock(account, context, until, stored.version());
        return Decision.UNLOCKED;
    }

    /** 잠금 대기·해시 계산 뒤 새 DB 시각으로 세션 버전과 기한을 확인한다. */
    private GuardianAccessRepository.GuardianAccessState currentState(AuthenticatedSession session) {
        var state = access.findState(session.accountId(), session.securityContextId())
                .orElseThrow(GuardianAccessException::authenticationRequired);
        if (!state.isSessionCurrent()) {
            throw GuardianAccessException.authenticationRequired();
        }
        return state;
    }

    /** 동시에 적용된 예산과 오답 차단 중 가장 늦은 해제를 안내한다. */
    private AuthRateLimitException retryAfter(List<AuthRateLimitKey> budgets, byte[] failureKey, long minimum) {
        return new AuthRateLimitException(Math.max(minimum,
                Math.max(limits.retryAfterSeconds(budgets), failures.pinBlockRetryAfterSeconds(failureKey))));
    }

    private enum Decision { UNLOCKED, INCORRECT, BLOCKED }
}
