package com.dodamdodam.backend.conversation.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** 저장된 시작 정보만 공개하며, 재전송 시 새 시각이나 대화 ID를 만들지 않는다. */
public record StartConversationResult(UUID conversationId, String status, OffsetDateTime startedAt,
                                      LocalDate serviceDate, OffsetDateTime scheduledEndAt) {

    /** 필수 공개 필드를 확인하고 같은 순간을 KST 초 정밀도로 표현한다. */
    public StartConversationResult {
        Objects.requireNonNull(conversationId, "conversationId");
        Objects.requireNonNull(serviceDate, "serviceDate");
        if (!"ACTIVE".equals(status)) {
            throw new IllegalArgumentException("시작 응답의 상태는 ACTIVE여야 합니다.");
        }
        // 수동으로 9시간을 더하지 않고 저장 시각의 오프셋만 변환한다.
        startedAt = Objects.requireNonNull(startedAt, "startedAt")
                .withOffsetSameInstant(ZoneOffset.ofHours(9)).truncatedTo(ChronoUnit.SECONDS);
        scheduledEndAt = Objects.requireNonNull(scheduledEndAt, "scheduledEndAt")
                .withOffsetSameInstant(ZoneOffset.ofHours(9)).truncatedTo(ChronoUnit.SECONDS);
    }
}
