package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.domain.TextVisibility;
import com.dodamdodam.backend.conversation.domain.TurnStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 필드별 허용 텍스트와 공개 처리 결과만 담으며 요청 키·해시·처리기한·세션 정보는 제외한다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record TurnView(
        UUID turnId,
        int sequence,
        TurnStatus status,
        String childText,
        TextVisibility childTextVisibility,
        String replyText,
        TextVisibility replyTextVisibility,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        OffsetDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        OffsetDateTime completedAt,
        String errorCode
) {
}
