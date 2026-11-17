package com.dodamdodam.backend.guardian.exception;

public final class GuardianAccessException extends RuntimeException {

    private final int status;
    private final String code;

    /** 정해진 권한 오류만 만들 수 있도록 상태·코드·원인을 내부에서 구성한다. */
    private GuardianAccessException(int status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    /** 현재 로그인 문맥이 없거나 유효하지 않은 경우를 표시한다. */
    public static GuardianAccessException authenticationRequired() {
        return new GuardianAccessException(401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.", null);
    }

    /** 현재 세션에 유효한 보호자 확인이 필요한 경우를 표시한다. */
    public static GuardianAccessException unlockRequired() {
        return new GuardianAccessException(403, "GUARDIAN_UNLOCK_REQUIRED", "보호자 PIN 확인이 필요합니다.", null);
    }

    /** 저장소 확인 실패의 원인은 보존하고 응답 메시지는 고정한다. */
    public static GuardianAccessException unavailable(Throwable cause) {
        return new GuardianAccessException(503, "AUTH_STATE_UNAVAILABLE",
                "인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.", cause);
    }

    /** API와 응답 재검사 경로가 사용할 HTTP 상태를 반환한다. */
    public int status() {
        return status;
    }

    /** 외부 응답에 사용할 고정 오류 코드를 반환한다. */
    public String code() {
        return code;
    }
}
