package com.dodamdodam.backend.conversation.infrastructure;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 발화의 원래 처리 기한을 운영 기본값 없이 명시적으로 받는다. */
@ConfigurationProperties("app.conversation.turn")
public record TurnProcessingProperties(Duration processingTimeout) {

    /** DB에 정확히 저장할 수 있는 양의 마이크로초 정밀도만 허용한다. */
    public TurnProcessingProperties {
        Objects.requireNonNull(processingTimeout, "발화 처리 기한을 명시적으로 설정해야 합니다.");
        if (processingTimeout.isZero() || processingTimeout.isNegative()
                || processingTimeout.getNano() % 1_000 != 0) {
            throw new IllegalArgumentException("발화 처리 기한은 양수이며 PostgreSQL 마이크로초 정밀도여야 합니다.");
        }
    }
}
