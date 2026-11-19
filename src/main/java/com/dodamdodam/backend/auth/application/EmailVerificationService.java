package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.dto.VerificationGrantResponse;
import com.dodamdodam.backend.auth.dto.VerifyEmailCodeRequest;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationAccountRepository;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationKeys;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.Challenge;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailVerificationService {
    private static final int MAX_WRONG_CODES = 5;
    private static final long TOKEN_TTL_SECONDS = 600;
    private static final Pattern TOKEN = Pattern.compile(
        "([0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12})\\.([A-Za-z0-9_-]{43})");
    private static final String PENDING_GRANT = EmailVerificationService.class.getName() + ".grant";

    private final EmailVerificationRepository repository;
    private final EmailVerificationAccountRepository accounts;
    private final SessionSecurityRepository sessions;
    private final AuthenticatedSessionService authentication;
    private final AuthRateLimitService limits;
    private final EmailVerificationKeys keys;
    private final TransactionTemplate work;
    private final SecureRandom random = new SecureRandom();

    /** 예산 예약과 검증 결과 저장을 독립된 트랜잭션으로 구성한다. */
    public EmailVerificationService(EmailVerificationRepository repository, EmailVerificationAccountRepository accounts,
                                    SessionSecurityRepository sessions, AuthenticatedSessionService authentication,
                                    AuthRateLimitService limits, EmailVerificationKeys keys, PlatformTransactionManager manager) {
        this.repository = repository;
        this.accounts = accounts;
        this.sessions = sessions;
        this.authentication = authentication;
        this.limits = limits;
        this.keys = keys;
        this.work = new TransactionTemplate(manager);
        this.work.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** IP와 목적별 예산을 먼저 확정하고, 실제 오답을 커밋한 뒤에 오류로 변환한다. */
    public VerificationGrantResponse verify(HttpServletRequest request, UUID id, Supplier<String> readCode, InetAddress address) {
        try {
            var rateHasher = keys.rateHasher();
            var apiKey = rateHasher.apiIp(address);
            var ipKey = rateHasher.emailVerifyIp(address);
            reserveVerificationBudget(ipKey, List.of(apiKey));
            var found = repository.findChallenge(id);

            // PIN challenge는 번호 타입과 추가 필드를 검사하기 전에 로그인을 확인한다.
            var current = found.isPresent() && isPin(found.get().purpose())
                ? authentication.requireAuthenticatedSession(request) : null;
            var input = new VerifyEmailCodeRequest(id, readCode.get());
            var initial = found.orElseThrow(EmailVerificationException::invalid);
            if (current != null) {
                // 대상 예산을 쓰기 전에 현재 권한을 확인하고, 잠금 대기 중 변경은 verifyLocked에서 다시 확인한다.
                checkPinContext(initial, current);
            } else if (initial.invalidatedAt() != null) {
                throw EmailVerificationException.invalid();
            }
            var subjectKey = rateHasher.emailVerifySubject(initial.purpose(), initial.email(), initial.setupAccountId());
            reserveVerificationBudget(subjectKey, List.of(apiKey, ipKey));

            var pending = PendingGrant.from(id, initial, current);
            var check = work.execute(status -> verifyLocked(input, pending, initial));
            if (check == null) {
                throw new AuthStateUnavailableException();
            }
            if (check.retryAfterSeconds() > 0) {
                // 다섯 번째 오답 뒤에는 다시 발급받아야 하므로 두 요청에 적용될 제한을 함께 확인한다.
                var retryKeys = new ArrayList<>(rateHasher.emailIssueKeys(initial.email(), initial.purpose(),
                    initial.setupAccountId(), initial.setupSessionBinding(), address));
                retryKeys.addAll(List.of(apiKey, ipKey, subjectKey));
                throw new AuthRateLimitException(Math.max(check.retryAfterSeconds(), limits.retryAfterSeconds(retryKeys)));
            }
            if (check.grant() == null) {
                throw EmailVerificationException.invalid();
            }
            request.setAttribute(PENDING_GRANT, pending);
            return check.grant();
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 현재 단계만 예약하고, 거부되면 앞서 사용한 예산까지 반영해 재시도 시간을 계산한다. */
    private void reserveVerificationBudget(AuthRateLimitKey key, List<AuthRateLimitKey> priorKeys) {
        try {
            limits.reserve(List.of(key));
        }
        catch (AuthRateLimitException rejected) {
            throw new AuthRateLimitException(Math.max(rejected.retryAfterSeconds(), limits.retryAfterSeconds(priorKeys)));
        }
    }

    /** 재설정 오답은 challenge만 잠그고, 발급 후보는 상위 권한도 먼저 잠근다. */
    private CodeCheck verifyLocked(VerifyEmailCodeRequest input, PendingGrant pending, Challenge initial) {
        var current = pending.current();
        var codeHash = keys.codeHasher().hash(input.code());
        boolean wrongReset = initial.purpose() == Purpose.RESET_PASSWORD
            && (!MessageDigest.isEqual(initial.codeHash(), codeHash) || !initial.eligible());
        var challenge = wrongReset
            ? repository.lockChallenge(input.challengeId()).orElseThrow(EmailVerificationException::invalid)
            : lockChallenge(pending, false);
        var now = current == null ? repository.databaseNow() : checkPinContext(challenge, current);
        if (challenge.invalidatedAt() != null || challenge.verifiedAt() != null
            || challenge.attemptCount() >= MAX_WRONG_CODES || !now.isBefore(challenge.expiresAt())) {
            throw EmailVerificationException.invalid();
        }
        boolean matches = MessageDigest.isEqual(challenge.codeHash(), codeHash);
        // 공개 decoy에도 일반 오답과 같은 제한을 적용하되 성공 상태는 만들지 않는다.
        if (!matches || !challenge.eligible() || wrongReset) {
            int attempts = challenge.attemptCount() + 1;
            boolean exhausted = attempts == MAX_WRONG_CODES;
            repository.recordWrongCode(input.challengeId(), attempts, exhausted ? now : null);
            return new CodeCheck(null, exhausted ? resendDelay(challenge.createdAt(), now) : 0);
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var expiresAt = now.plusSeconds(TOKEN_TTL_SECONDS);
        repository.storeGrant(input.challengeId(), now, digest(secret), expiresAt);
        return new CodeCheck(new VerificationGrantResponse(input.challengeId() + "." + secret,
            expiresAt.withOffsetSameInstant(ZoneOffset.ofHours(9))), 0);
    }

    /** 커밋 뒤 응답이 늦어진 동안 세션·PIN·토큰이 바뀌었으면 성공 본문을 보내지 않는다. */
    public void verifyResponseAccess(HttpServletRequest request) {
        if (!(request.getAttribute(PENDING_GRANT) instanceof PendingGrant pending)) {
            return;
        }
        work.executeWithoutResult(status -> {
            var current = pending.current();
            var challenge = lockChallenge(pending, false);
            var now = current == null ? repository.databaseNow() : checkPinContext(challenge, current);
            requireActiveToken(challenge, now);
        });
    }

    /** 발급·응답 재검사·소비 모두 계정을 먼저 잠그고 같은 reset 발급 시점의 버전을 확인한다. */
    private Challenge lockChallenge(PendingGrant pending, boolean lockPinSetupProofs) {
        if (pending.current() != null) {
            lockPinContext(pending.current());
            // 최초 설정은 대기 중 만료도 확인하도록 관련 증명을 먼저 ID 순서로 잠근다.
            if (lockPinSetupProofs) {
                repository.lockPinSetupProofs(pending.current().accountId(), pending.id());
            }
        }
        var reset = pending.resetEmail() == null ? null
            : accounts.findForUpdate(pending.resetEmail()).orElseThrow(EmailVerificationException::invalid);
        var challenge = repository.lockChallenge(pending.id()).orElseThrow(EmailVerificationException::invalid);
        if (reset != null && (!reset.id().equals(challenge.resetAccountId())
            || !Objects.equals(reset.sessionVersion(), challenge.resetSessionVersion()))) {
            throw EmailVerificationException.invalid();
        }
        return challenge;
    }

    /** PIN 목적은 계정 → 문맥 → PIN 순서로 잠근 뒤 challenge에 접근한다. */
    private void lockPinContext(AuthenticatedSession current) {
        try {
            sessions.requireCurrentContextForUpdate(current.accountId(), current.securityContextId());
        }
        catch (InsufficientAuthenticationException revoked) {
            throw EmailVerificationException.authenticationRequired();
        }
        accounts.lockPin(current.accountId());
    }

    /** 다른 계정·보안 문맥이 요청한 실제 PIN challenge는 403으로 구분한다. */
    private void requirePinBinding(Challenge challenge, AuthenticatedSession current) {
        if (!current.accountId().equals(challenge.setupAccountId())
            || !current.securityContextId().equals(challenge.setupSessionBinding())) {
            throw EmailVerificationException.authorizationRequired(challenge.purpose());
        }
    }

    /** 실제 PIN 권한에 묶인 이메일·generation·PIN 버전은 번호나 토큰 만료보다 먼저 확인한다. */
    private OffsetDateTime checkPinContext(Challenge challenge, AuthenticatedSession current) {
        requirePinBinding(challenge, current);
        var state = accounts.findPinState(current.accountId(), current.securityContextId())
            .orElseThrow(EmailVerificationException::authenticationRequired);
        if (!state.isSessionCurrent()) {
            throw EmailVerificationException.authenticationRequired();
        }
        if (challenge.invalidatedAt() != null || !challenge.email().equals(state.email())
            || state.setupGeneration() != challenge.setupGeneration()) {
            throw EmailVerificationException.authorizationRequired(challenge.purpose());
        }
        if (challenge.purpose() == Purpose.PIN_SETUP && state.pinVersion() != null) {
            throw EmailVerificationException.pinAlreadySet();
        }
        if (challenge.purpose() == Purpose.PIN_RESET) {
            if (state.pinVersion() == null) {
                throw EmailVerificationException.pinNotSet();
            }
            if (!Objects.equals(state.pinVersion(), challenge.pinVersionSnapshot())) {
                throw EmailVerificationException.authorizationRequired(challenge.purpose());
            }
        }
        return state.checkedAt();
    }

    /** 토큰의 기한·폐기·소비 상태를 검증한다. */
    private void requireActiveToken(Challenge challenge, OffsetDateTime now) {
        if (challenge.invalidatedAt() != null || challenge.tokenConsumedAt() != null
            || challenge.tokenExpiresAt() == null || !now.isBefore(challenge.tokenExpiresAt())) {
            throw EmailVerificationException.invalid();
        }
    }

    /** 두 PIN 목적에만 로그인 및 security context 결합을 적용한다. */
    private boolean isPin(Purpose purpose) {
        return purpose == Purpose.PIN_SETUP || purpose == Purpose.PIN_RESET;
    }

    /** 재사용이 불가능한 번호 대신 재발급까지 남은 초를 올림한다. */
    private long resendDelay(OffsetDateTime createdAt, OffsetDateTime now) {
        var remaining = Duration.between(now, createdAt.plusSeconds(Action.EMAIL_RESEND.windowSeconds()));
        return Math.max(1, remaining.getSeconds() + (remaining.getNano() == 0 ? 0 : 1));
    }

    /** DB에는 Base64url secret 문자열의 SHA-256 검증값만 남긴다. */
    private byte[] digest(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 기존 PIN 유무를 확인하기 전에 요청 토큰의 구조만 검증한다. */
    public static boolean isTokenFormatValid(String token) {
        return token != null && TOKEN.matcher(token).matches();
    }

    /** 가입·비밀번호·PIN 업무의 기존 트랜잭션에서만 해당 목적의 토큰을 소비한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public ConsumedVerification consume(String token, Purpose expectedPurpose, AuthenticatedSession current) {
        var match = TOKEN.matcher(token == null ? "" : token);
        if (!match.matches()) {
            throw EmailVerificationException.validation("verificationToken");
        }
        var id = UUID.fromString(match.group(1));
        byte[] hash = digest(match.group(2));

        var initial = repository.findChallenge(id).orElseThrow(EmailVerificationException::invalid);
        requireMatchingToken(initial, expectedPurpose, hash);
        boolean pin = isPin(expectedPurpose);
        if (pin) {
            if (current == null) {
                throw EmailVerificationException.authenticationRequired();
            }
            requirePinBinding(initial, current);
        }
        var pending = PendingGrant.from(id, initial, pin ? current : null);
        var challenge = lockChallenge(pending, expectedPurpose == Purpose.PIN_SETUP);
        requireMatchingToken(challenge, expectedPurpose, hash);
        var now = pin ? checkPinContext(challenge, current) : repository.databaseNow();
        requireActiveToken(challenge, now);

        repository.markConsumed(id, now);
        return new ConsumedVerification(challenge.email(), challenge.resetAccountId());
    }

    /** 목적이나 해시가 다른 입력에는 실제 토큰의 상태를 알려주지 않는다. */
    private void requireMatchingToken(Challenge challenge, Purpose expectedPurpose, byte[] hash) {
        if (challenge.purpose() != expectedPurpose || !MessageDigest.isEqual(challenge.tokenHash(), hash)) {
            throw EmailVerificationException.invalid();
        }
    }

    /** 업무 호출자에게 인증된 이메일과 비밀번호 재설정 대상만 넘긴다. */
    public record ConsumedVerification(String email, UUID resetAccountId) { }

    private record CodeCheck(VerificationGrantResponse grant, long retryAfterSeconds) { }
    private record PendingGrant(UUID id, AuthenticatedSession current, String resetEmail) {
        /** 공개 decoy는 계정 잠금이나 존재 여부 검사 없이 일반 오답으로 처리한다. */
        static PendingGrant from(UUID id, Challenge initial, AuthenticatedSession current) {
            String resetEmail = initial.purpose() == Purpose.RESET_PASSWORD && initial.eligible() ? initial.email() : null;
            return new PendingGrant(id, current, resetEmail);
        }
    }
}
