package com.dodamdodam.backend.guardian.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.guardian.exception.GuardianAccessException;
import com.dodamdodam.backend.guardian.infrastructure.GuardianAccessRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;

@Service
public class GuardianAccessService {

    private static final String RESPONSE_ACCESS = GuardianAccessService.class.getName() + ".responseAccess";

    private final GuardianAccessRepository repository;

    /** 현재 DB 권한을 조회할 저장소를 구성한다. */
    public GuardianAccessService(GuardianAccessRepository repository) {
        this.repository = repository;
    }

    /** 같은 요청의 인증·소유권 검사 뒤 보호자 확인 기한을 요구한다. */
    public OffsetDateTime requireGuardianAccess(HttpServletRequest request, AuthenticatedSession authenticated) {
        var until = requireCurrentAccess(authenticated);
        // 성공한 계정·문맥만 요청에 남기며 HTTP 세션이나 DB 권한을 연장하지 않는다.
        request.setAttribute(RESPONSE_ACCESS, authenticated);
        return until;
    }

    /** 재검사 대상 요청인지만 반환하며, 표식 자체가 현재 권한을 보증하지는 않는다. */
    public static boolean requiresResponseVerification(HttpServletRequest request) {
        return request.getAttribute(RESPONSE_ACCESS) instanceof AuthenticatedSession;
    }

    /** 보호자 권한 검사에 성공한 요청의 현재 DB 권한을 다시 확인한다. */
    public void verifyResponseAccess(HttpServletRequest request) {
        if (request.getAttribute(RESPONSE_ACCESS) instanceof AuthenticatedSession authenticated) {
            // 표식을 유지해 반복 검사에서도 저장 당시 기한 대신 현재 DB 상태를 사용한다.
            requireCurrentAccess(authenticated);
        }
    }

    /** 로그인 문맥 유효성을 먼저 확인한 뒤 같은 문맥의 최신 보호자 권한을 요구한다. */
    private OffsetDateTime requireCurrentAccess(AuthenticatedSession authenticated) {
        var state = repository.findState(authenticated.accountId(), authenticated.securityContextId())
                .orElseThrow(GuardianAccessException::authenticationRequired);
        if (!state.isSessionCurrent()) {
            throw GuardianAccessException.authenticationRequired();
        }
        if (!state.isGuardianCurrent()) {
            throw GuardianAccessException.unlockRequired();
        }
        return state.guardianUntil();
    }
}
