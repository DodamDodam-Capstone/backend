package com.dodamdodam.backend.auth.exception;

public final class AuthRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    /** 식별자·DB 상태 없이 최소 재시도 대기시간만 전달한다. */
    public AuthRateLimitException(long retryAfterSeconds) {
        super("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    /** HTTP 연결부에서 사용할 고정 업무 오류 코드를 반환한다. */
    public String code() {
        return "RATE_LIMITED";
    }

    /** 가장 늦은 제한이 끝나는 시각까지 올림한 대기 초를 반환한다. */
    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
