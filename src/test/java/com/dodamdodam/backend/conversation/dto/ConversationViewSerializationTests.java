package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.application.AllowedTextPolicy;
import com.dodamdodam.backend.conversation.domain.ConversationStatus;
import com.dodamdodam.backend.conversation.domain.EndReason;
import com.dodamdodam.backend.conversation.domain.SummaryStatus;
import com.dodamdodam.backend.conversation.domain.TextVisibility;
import com.dodamdodam.backend.conversation.domain.TurnStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationViewSerializationTests {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));
    private static final UUID ID = UUID.fromString("55555555-5555-4555-8555-555555555555");
    private static final UUID CHILD_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-03T14:59:59Z");

    @Test
    void processingTurnKeepsNullFieldsEvenWhenMapperOmitsNullsByDefault() {
        contextRunner.withPropertyValues("spring.jackson.default-property-inclusion=non_null").run(context -> {
            var turn = new TurnView(ID, 1, TurnStatus.PROCESSING,
                    null, TextVisibility.OMITTED, null, TextVisibility.OMITTED, CREATED_AT, null, null);
            var json = json(context.getBean(JsonMapper.class), turn);

            assertThat(json).containsOnlyKeys("turnId", "sequence", "status", "childText",
                    "childTextVisibility", "replyText", "replyTextVisibility", "createdAt", "completedAt", "errorCode");
            assertThat(json).containsEntry("turnId", ID.toString()).containsEntry("sequence", 1)
                    .containsEntry("status", "PROCESSING")
                    .containsEntry("childText", null).containsEntry("childTextVisibility", "OMITTED")
                    .containsEntry("replyText", null).containsEntry("replyTextVisibility", "OMITTED")
                    .containsEntry("completedAt", null).containsEntry("errorCode", null)
                    .containsEntry("createdAt", "2026-10-03T23:59:59+09:00");
        });
    }

    @ParameterizedTest
    @EnumSource(value = TurnStatus.class, names = {"SUCCEEDED", "FAILED"})
    void terminalTurnSerializesAllowedTextAndCompletionInKst(TurnStatus status) {
        contextRunner.run(context -> {
            var errorCode = status == TurnStatus.FAILED ? "AI_UPSTREAM_FAILED" : null;
            var turn = new TurnView(ID, 2, status, "오늘 친구랑 놀았어.", TextVisibility.VISIBLE,
                    "친구와 놀았구나.", TextVisibility.REDACTED, CREATED_AT, CREATED_AT.plusSeconds(2), errorCode);
            var json = json(context.getBean(JsonMapper.class), turn);

            assertThat(json).hasSize(10).containsEntry("status", status.name())
                    .containsEntry("childText", "오늘 친구랑 놀았어.").containsEntry("childTextVisibility", "VISIBLE")
                    .containsEntry("replyText", "친구와 놀았구나.").containsEntry("replyTextVisibility", "REDACTED")
                    .containsEntry("completedAt", "2026-10-04T00:00:01+09:00")
                    .containsEntry("errorCode", errorCode);
        });
    }

    @Test
    void activeConversationKeepsAllNullableFields() {
        contextRunner.withPropertyValues("spring.jackson.default-property-inclusion=non_null").run(context -> {
            var conversation = new ConversationView(ID, CHILD_ID, ConversationStatus.ACTIVE, CREATED_AT,
                    null, null, null, null, null, null, SummaryStatus.NOT_STARTED);
            var json = json(context.getBean(JsonMapper.class), conversation);

            assertThat(json).containsOnlyKeys("conversationId", "childId", "status", "startedAt", "endRequestedAt",
                    "endReason", "endedAt", "title", "topic", "summary", "summaryStatus");
            assertThat(json).containsEntry("conversationId", ID.toString()).containsEntry("childId", CHILD_ID.toString())
                    .containsEntry("status", "ACTIVE").containsEntry("startedAt", "2026-10-03T23:59:59+09:00")
                    .containsEntry("endRequestedAt", null).containsEntry("endReason", null)
                    .containsEntry("endedAt", null).containsEntry("title", null).containsEntry("topic", null)
                    .containsEntry("summary", null).containsEntry("summaryStatus", "NOT_STARTED");
        });
    }

    @Test
    void closingConversationSerializesManualBoundaryWithoutEndedAt() {
        contextRunner.run(context -> {
            var startedAt = CREATED_AT.minusHours(1);
            var conversation = new ConversationView(ID, CHILD_ID, ConversationStatus.CLOSING, startedAt,
                    CREATED_AT, EndReason.MANUAL, null, null, null, null, SummaryStatus.NOT_STARTED);
            var json = json(context.getBean(JsonMapper.class), conversation);

            assertThat(json).containsEntry("status", "CLOSING").containsEntry("endReason", "MANUAL")
                    .containsEntry("startedAt", "2026-10-03T22:59:59+09:00")
                    .containsEntry("endRequestedAt", "2026-10-03T23:59:59+09:00").containsEntry("endedAt", null);
        });
    }

    @ParameterizedTest
    @EnumSource(value = SummaryStatus.class, names = {"PENDING", "READY", "FAILED", "EMPTY"})
    void endedConversationSerializesMidnightBoundaryAndSummaryStatus(SummaryStatus summaryStatus) {
        contextRunner.run(context -> {
            boolean ready = summaryStatus == SummaryStatus.READY;
            var conversation = new ConversationView(ID, CHILD_ID, ConversationStatus.ENDED, CREATED_AT,
                    CREATED_AT.plusSeconds(1), EndReason.MIDNIGHT, CREATED_AT.plusSeconds(2),
                    ready ? "친구 이야기" : null, ready ? "놀이" : null, ready ? "친구와 놀았다." : null, summaryStatus);
            var json = json(context.getBean(JsonMapper.class), conversation);

            assertThat(json).hasSize(11).containsEntry("status", "ENDED").containsEntry("endReason", "MIDNIGHT")
                    .containsEntry("endRequestedAt", "2026-10-04T00:00:00+09:00")
                    .containsEntry("endedAt", "2026-10-04T00:00:01+09:00")
                    .containsEntry("summaryStatus", summaryStatus.name())
                    .containsEntry("title", ready ? "친구 이야기" : null)
                    .containsEntry("topic", ready ? "놀이" : null)
                    .containsEntry("summary", ready ? "친구와 놀았다." : null);
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(JsonMapper mapper, Object value) {
        return mapper.readValue(mapper.writeValueAsString(value), Map.class);
    }

    @Test
    void policyRejectedOriginalIsAbsentFromSerializedTurnAndDiagnostics() {
        contextRunner.run(context -> {
            var mapper = context.getBean(JsonMapper.class);
            var policy = new AllowedTextPolicy();
            String forbidden = "synthetic forbidden original";
            var child = policy.select(TextVisibility.VISIBLE, false, true, forbidden);
            var reply = policy.select(TextVisibility.VISIBLE, true, true, "[test] permitted reply");
            var turn = new TurnView(ID, 1, TurnStatus.SUCCEEDED, child.text(), child.visibility(),
                    reply.text(), reply.visibility(), CREATED_AT, CREATED_AT.plusSeconds(2), null);

            assertThat(json(mapper, turn)).containsEntry("childText", null)
                    .containsEntry("childTextVisibility", "OMITTED")
                    .containsEntry("replyText", "[test] permitted reply")
                    .containsEntry("replyTextVisibility", "VISIBLE");
            assertThat(mapper.writeValueAsString(turn)).doesNotContain(forbidden);
            assertThat(turn.toString()).doesNotContain(forbidden);
        });
    }

    @Test
    void successfulAndFailedJsonSamplesMatchDtoContractWithSequentialTimes() {
        contextRunner.run(context -> {
            var mapper = context.getBean(JsonMapper.class);
            var succeeded = new TurnView(ID, 1, TurnStatus.SUCCEEDED,
                    null, TextVisibility.OMITTED, "[test] permitted reply", TextVisibility.VISIBLE,
                    OffsetDateTime.parse("2026-10-03T14:09:00+09:00"),
                    OffsetDateTime.parse("2026-10-03T14:09:02+09:00"), null);
            var failed = new TurnView(UUID.fromString("66666666-6666-4666-8666-666666666666"), 2, TurnStatus.FAILED,
                    "[test] permitted child", TextVisibility.VISIBLE, null, TextVisibility.OMITTED,
                    OffsetDateTime.parse("2026-10-03T14:09:20+09:00"),
                    OffsetDateTime.parse("2026-10-03T14:09:24+09:00"), "AI_UPSTREAM_FAILED");

            assertThat(json(mapper, succeeded)).isEqualTo(sample(mapper, "turn-succeeded.json"));
            assertThat(json(mapper, failed)).isEqualTo(sample(mapper, "turn-failed.json"));
            assertThat(json(mapper, succeeded)).containsOnlyKeys("turnId", "sequence", "status", "childText",
                    "childTextVisibility", "replyText", "replyTextVisibility", "createdAt", "completedAt", "errorCode");
            assertThat(json(mapper, failed)).containsOnlyKeys("turnId", "sequence", "status", "childText",
                    "childTextVisibility", "replyText", "replyTextVisibility", "createdAt", "completedAt", "errorCode");
            assertThat(succeeded.completedAt()).isAfter(succeeded.createdAt());
            assertThat(failed.createdAt()).isAfter(succeeded.completedAt());
            assertThat(failed.completedAt()).isAfter(failed.createdAt());
            assertThat(failed.sequence()).isGreaterThan(succeeded.sequence());
            assertThat(failed.turnId()).isNotEqualTo(succeeded.turnId());
            assertThat(succeeded.errorCode()).isNull();
            assertThat(failed.errorCode()).isEqualTo("AI_UPSTREAM_FAILED").isNotBlank();
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sample(JsonMapper mapper, String filename) {
        try (var input = ConversationViewSerializationTests.class.getResourceAsStream(
                "/com/dodamdodam/backend/conversation/dto/" + filename)) {
            assertThat(input).as("synthetic TurnView sample").isNotNull();
            return mapper.readValue(input, Map.class);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read synthetic TurnView sample", exception);
        }
    }
}
