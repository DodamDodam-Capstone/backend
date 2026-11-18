package com.dodamdodam.backend.auth.domain;

/** 원문 식별자 대신 목적과 32바이트 HMAC으로 예약 대상을 구분한다. */
public record AuthRateLimitKey(Action action, byte[] keyHash) {

    /** DB에 전달할 해시를 검증·복사해 호출자의 배열 변경으로 범위가 바뀌지 않게 한다. */
    public AuthRateLimitKey {
        if (action == null || keyHash == null || keyHash.length != 32) {
            throw new IllegalArgumentException("요청 제한 키가 올바르지 않습니다.");
        }
        keyHash = keyHash.clone();
    }

    /** 해시 원본을 노출하지 않아 같은 예약의 식별값을 유지한다. */
    @Override
    public byte[] keyHash() {
        return keyHash.clone();
    }

    /** 기존 기술 기준의 고정창 요청량이며 PIN 실제 오답 차단과 구분한다. */
    public enum Action {
        API_IP(300, 60),
        LOGIN_EMAIL(10, 900),
        LOGIN_IP(100, 900),
        EMAIL_SEND_SUBJECT(5, 900),
        EMAIL_SEND_IP(20, 900),
        EMAIL_RESEND(1, 60),
        EMAIL_VERIFY_IP(100, 900),
        EMAIL_VERIFY_SUBJECT(10, 900);

        private final long capacity;
        private final long windowSeconds;

        /** 창 안에서 허용하는 예약 수와 창 길이를 보관한다. */
        Action(long capacity, long windowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
        }

        /** 한도번째 요청까지 허용하는 예약 수를 반환한다. */
        public long capacity() {
            return capacity;
        }

        /** 최초 예약부터 고정하는 요청창 길이를 초로 반환한다. */
        public long windowSeconds() {
            return windowSeconds;
        }
    }
}
