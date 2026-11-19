package com.dodamdodam.backend.home.dto;

import com.dodamdodam.backend.child.dto.ChildContext;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 아이 홈의 공개 7필드이며 대화 본문·음성·종료 확인 정보는 포함하지 않는다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ChildHome(
        Child child,
        String greeting,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
        OffsetDateTime joinedAt,
        long daysTogether,
        UUID activeConversationId,
        List<Menu> menus,
        Availability availability
) {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 호출자는 소유권과 후보 ACTIVE ID의 당일·현재 세션 연결을 먼저 검증해야 한다. */
    public static ChildHome from(ChildContext context, OffsetDateTime databaseNow,
            UUID accessibleActiveConversationId, boolean hasClosingConversation) {
        Objects.requireNonNull(context, "context");
        var checkedAt = Objects.requireNonNull(databaseNow, "databaseNow").atZoneSameInstant(KST);
        var registeredAt = context.createdAt().atZoneSameInstant(KST);
        var serviceDate = checkedAt.toLocalDate();
        long daysTogether = ChronoUnit.DAYS.between(registeredAt.toLocalDate(), serviceDate) + 1;
        if (daysTogether < 1) {
            throw new IllegalArgumentException("판정 날짜는 아이 등록 날짜보다 빠를 수 없습니다.");
        }

        var opensAt = serviceDate.atTime(8, 0).atZone(KST).toOffsetDateTime();
        var closesAt = serviceDate.plusDays(1).atStartOfDay(KST).toOffsetDateTime();
        // 시간 외에는 CLOSING 여부나 기존 연결과 무관하게 다음 개방 시각을 안내한다.
        boolean outsideHours = !ConversationServiceHours.isOpen(databaseNow);
        boolean canEnter = !outsideHours && !hasClosingConversation;
        String reason = outsideHours ? "OUTSIDE_SERVICE_HOURS"
                : hasClosingConversation ? "CONVERSATION_CLOSING" : null;
        var availability = new Availability(KST.getId(), checkedAt.toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS),
                serviceDate, opensAt, closesAt, canEnter, reason, outsideHours ? opensAt : null);

        return new ChildHome(new Child(context.childId(), context.name(), context.nickname(), context.characterId()),
                context.nickname() + ", 오늘도 반가워!", registeredAt.toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS),
                daysTogether, canEnter ? accessibleActiveConversationId : null,
                List.of(new Menu("TALK", canEnter ? "AVAILABLE" : "UNAVAILABLE"), new Menu("PLAY", "COMING_SOON")),
                availability);
    }

    /** 내부 자녀 문맥에서 홈 표시용 최소 프로필만 공개한다. */
    public record Child(UUID childId, String name, String nickname, String characterId) { }

    /** TALK·PLAY의 고정 순서와 상태를 표현한다. */
    public record Menu(String code, String status) { }

    /** 같은 판정 시각에서 만든 시간표이며 nullable 필드는 전역 설정과 무관하게 유지한다. */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Availability(
            String timeZone,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
            OffsetDateTime serverTime,
            LocalDate serviceDate,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
            OffsetDateTime opensAt,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
            OffsetDateTime closesAt,
            boolean canEnter,
            String reason,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul")
            OffsetDateTime nextOpensAt
    ) { }
}
