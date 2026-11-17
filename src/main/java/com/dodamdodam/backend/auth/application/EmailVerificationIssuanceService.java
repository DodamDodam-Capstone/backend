package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.dto.*;
import com.dodamdodam.backend.auth.dto.IssueEmailVerificationRequest.Purpose;
import com.dodamdodam.backend.auth.infrastructure.*;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository.*;
import com.dodamdodam.backend.auth.session.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.net.InetAddress;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Objects;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EmailVerificationIssuanceService {
    private final EmailVerificationRepository repository;
    private final EmailVerificationAccountRepository accounts;
    private final EmailVerificationKeys keys;
    private final AuthRateLimitService limits;
    private final EmailVerificationDeliveryService delivery;
    private final Validator validator;
    private final TransactionTemplate work;
    private final SessionSecurityRepository sessions;
    private static final String RESPONSE = EmailVerificationIssuanceService.class.getName() + ".response";
    private final EmailVerificationCodeGenerator codes = new EmailVerificationCodeGenerator();

    /** 예산 예약·업무 저장·외부 발송을 서로 다른 경계로 연결한다. */
    public EmailVerificationIssuanceService(EmailVerificationRepository repository, EmailVerificationAccountRepository accounts,
            EmailVerificationKeys keys, AuthRateLimitService limits, EmailVerificationDeliveryService delivery,
            Validator validator, PlatformTransactionManager manager, SessionSecurityRepository sessions) {
        this.repository = repository;
        this.accounts = accounts;
        this.keys = keys;
        this.limits = limits;
        this.delivery = delivery;
        this.validator = validator;
        this.sessions = sessions;
        this.work = new TransactionTemplate(manager);
        this.work.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 구조 검증 뒤 예산을 먼저 커밋하고, challenge 커밋이 끝난 다음에만 메일을 발송한다. */
    public VerificationIssueResponse issue(HttpServletRequest request, IssueEmailVerificationRequest input,
                                          AuthenticatedSession current, InetAddress address) {
        boolean pin = input.purpose() == Purpose.PIN_SETUP || input.purpose() == Purpose.PIN_RESET;
        if (pin && current == null) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        var normalized = input.normalized(validator);
        limits.reserve(keys.rateHasher().emailIssueKeys(normalized.email(), normalized.purpose(),
                pin ? current.accountId() : null, pin ? current.securityContextId() : null, address));
        String code = codes.generate();
        byte[] hash = keys.codeHasher().hash(code);
        Prepared prepared;
        try {
            prepared = work.execute(status -> pin ? preparePin(normalized, current, hash) : preparePublic(normalized, hash));
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
        if (prepared == null) {
            throw new AuthStateUnavailableException();
        }
        request.setAttribute(RESPONSE, prepared);
        delivery.deliver(prepared.issued().challengeId(), prepared.data().email(), code, prepared.data().eligible());
        verifyResponseAccess(request);
        return new VerificationIssueResponse(prepared.issued().challengeId(),
                prepared.issued().expiresAt().withOffsetSameInstant(ZoneOffset.ofHours(9)));
    }

    /** 같은 이메일·목적을 잠근 뒤 현재 계정으로 normal/decoy와 reset 버전을 판정한다. */
    private Prepared preparePublic(IssueEmailVerificationRequest input, byte[] hash) {
        repository.lockScope(input.email(), input.purpose(), null, null);
        var account = accounts.findForUpdate(input.email());
        boolean eligible = input.purpose() == Purpose.SIGNUP ? account.isEmpty() : account.isPresent();
        repository.requireResendReady(input.email(), input.purpose(), null, null);
        var reset = input.purpose() == Purpose.RESET_PASSWORD && eligible ? account.orElseThrow() : null;
        var data = new NewChallenge(UUID.randomUUID(), input.email(), input.purpose(), eligible, hash,
                null, null, null, null, reset == null ? null : reset.id(), reset == null ? null : reset.sessionVersion());
        return new Prepared(data, repository.replace(data));
    }

    /** 계정→문맥→PIN→challenge 순서로 잠그고 현재 이메일·generation·PIN 버전을 저장한다. */
    private Prepared preparePin(IssueEmailVerificationRequest input, AuthenticatedSession current, byte[] hash) {
        sessions.requireCurrentContextForUpdate(current.accountId(), current.securityContextId());
        accounts.lockPin(current.accountId());
        var state = accounts.findPinState(current.accountId(), current.securityContextId()).orElseThrow(
                EmailVerificationException::authenticationRequired);
        if (!state.isSessionCurrent()) {
            throw EmailVerificationException.authenticationRequired();
        }
        String email = new IssueEmailVerificationRequest(Purpose.SIGNUP, state.email()).normalized(validator).email();
        if (input.email() != null && !input.email().equals(email)) {
            throw EmailVerificationException.validation("email");
        }
        if (input.purpose() == Purpose.PIN_SETUP && state.pinVersion() != null) {
            throw EmailVerificationException.pinAlreadySet();
        }
        if (input.purpose() == Purpose.PIN_RESET && state.pinVersion() == null) {
            throw EmailVerificationException.pinNotSet();
        }
        repository.lockScope(email, input.purpose(), current.accountId(), current.securityContextId());
        repository.requireResendReady(email, input.purpose(), current.accountId(), current.securityContextId());
        var data = new NewChallenge(UUID.randomUUID(), email, input.purpose(), true, hash, current.accountId(),
                current.securityContextId(), state.setupGeneration(), input.purpose() == Purpose.PIN_RESET ? state.pinVersion() : null,
                null, null);
        return new Prepared(data, repository.replace(data));
    }

    /** 전송 직전 현재 DB를 읽고 늦은 폐기·계정/문맥/PIN 변경이면 이 발급만 폐기한다. */
    public void verifyResponseAccess(HttpServletRequest request) {
        if (!(request.getAttribute(RESPONSE) instanceof Prepared prepared)) {
            return;
        }
        try {
            var data = prepared.data();
            if (data.setupAccountId() != null) {
                var state = accounts.findPinState(data.setupAccountId(), data.setupSessionBinding());
                if (state.isEmpty() || !state.get().isSessionCurrent()) {
                    delivery.invalidate(data.id());
                    throw EmailVerificationException.authenticationRequired();
                }
                var current = state.get();
                boolean pinMatches = data.purpose() == Purpose.PIN_SETUP ? current.pinVersion() == null
                        : Objects.equals(current.pinVersion(), data.pinVersionSnapshot());
                if (!data.email().equals(current.email()) || data.setupGeneration() != current.setupGeneration() || !pinMatches) {
                    delivery.invalidate(data.id());
                    throw EmailVerificationException.authorizationRequired(data.purpose());
                }
            }
            if (!repository.isIssueResponseCurrent(data.id())) {
                delivery.invalidate(data.id());
                throw EmailVerificationException.deliveryUnavailable();
            }
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 내부 적격 값은 HTTP 응답에 포함하지 않는다. */
    private record Prepared(NewChallenge data, IssuedChallenge issued) { }
}
