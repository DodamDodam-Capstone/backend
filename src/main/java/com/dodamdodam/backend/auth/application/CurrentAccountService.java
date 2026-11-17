package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.dto.AccountView;
import com.dodamdodam.backend.auth.infrastructure.CurrentAccountRepository;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class CurrentAccountService {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private final CurrentAccountRepository repository;
    private final Validator validator;

    /** 현재 계정 조회와 기존 출력 제약 검사를 연결한다. */
    public CurrentAccountService(CurrentAccountRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    /** 프레임워크 인증을 통과한 문맥의 현재 계정 상태를 반환하며 보호자 권한을 요구·연장하지 않는다. */
    public AccountView getCurrentAccount(AuthenticatedSession session) {
        var state = repository.findState(session.accountId(), session.securityContextId())
                .filter(current -> current.access().isSessionCurrent())
                .orElseThrow(() -> new InsufficientAuthenticationException("로그인이 필요합니다."));
        var access = state.access();
        OffsetDateTime guardianUntil = null;
        if (access.isGuardianCurrent()) {
            // 초 단위 표시가 실제 기한보다 길어지지 않게 내리고 이미 지난 표시 시각은 숨긴다.
            var visibleUntil = access.guardianUntil().withOffsetSameInstant(KST).truncatedTo(ChronoUnit.SECONDS);
            if (access.checkedAt().isBefore(visibleUntil)) {
                guardianUntil = visibleUntil;
            }
        }
        var view = new AccountView(state.accountId(), state.email(), state.childId(),
                access.currentPinVersion() != null, guardianUntil);
        if (!validator.validate(view).isEmpty()) {
            // 잘못된 저장 이메일을 정상 응답으로 노출하거나 계정 없음으로 바꾸지 않는다.
            throw new AuthStateUnavailableException();
        }
        return view;
    }
}
