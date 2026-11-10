package com.dodamdodam.backend.auth.session;

public final class AuthStateUnavailableException extends RuntimeException {

    /** 로그인 저장 결과를 확인할 수 없는 상태를 인증 장애로 표시한다. */
    public AuthStateUnavailableException() {
        this(null);
    }

    /** 원인은 보존하되 외부 응답에 사용할 메시지는 고정한다. */
    public AuthStateUnavailableException(Throwable cause) {
        super("인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.", cause);
    }
}
