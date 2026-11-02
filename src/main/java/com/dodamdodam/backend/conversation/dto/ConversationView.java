package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.domain.ConversationStatus;
import com.dodamdodam.backend.conversation.domain.EndReason;
import com.dodamdodam.backend.conversation.domain.SummaryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;

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
