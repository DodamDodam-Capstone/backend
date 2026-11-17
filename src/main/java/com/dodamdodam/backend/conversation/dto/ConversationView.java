package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.domain.ConversationStatus;
import com.dodamdodam.backend.conversation.domain.EndReason;
import com.dodamdodam.backend.conversation.domain.SummaryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 공개 읽기 모델 11필드만 담으며 시작 키·서비스 날짜·요약 작업 내부 정보는 포함하지 않는다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConversationView(
        UUID conversationId,
        UUID childId,
        ConversationStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        OffsetDateTime startedAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        OffsetDateTime endRequestedAt,
        EndReason endReason,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        OffsetDateTime endedAt,
        String title,
        String topic,
        String summary,
        SummaryStatus summaryStatus
) {
}
