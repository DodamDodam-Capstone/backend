package com.dodamdodam.backend.conversation.infrastructure;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 미확정 요약 처리 기한은 운영 기본값 없이 명시적으로 주입한다. */
@ConfigurationProperties("app.conversation")
public record ConversationClosureProperties(Duration summaryProcessingTimeout) {

    /** NULL·0·음수와 PostgreSQL 마이크로초보다 세밀한 정밀도를 거부한다. */
    public ConversationClosureProperties {
        Objects.requireNonNull(summaryProcessingTimeout, "요약 처리 기한을 명시적으로 설정해야 합니다.");
        if (summaryProcessingTimeout.isZero() || summaryProcessingTimeout.isNegative()
                || summaryProcessingTimeout.getNano() % 1_000 != 0) {
            throw new IllegalArgumentException("요약 처리 기한은 양수이며 PostgreSQL 마이크로초 정밀도여야 합니다.");
        }
    }
}
