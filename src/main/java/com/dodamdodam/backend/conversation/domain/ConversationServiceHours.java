package com.dodamdodam.backend.conversation.domain;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;

public final class ConversationServiceHours {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalTime OPENS_AT = LocalTime.of(8, 0);

    /** 외부 시계 없이 전달받은 DB 판정 시각만 사용하는 정책이다. */
    private ConversationServiceHours() {
    }

    /** 호출자가 잠금 후 읽은 DB 시각으로 08시 포함·다음 자정 미포함을 판정한다. */
    public static ServiceWindow requireOpen(OffsetDateTime databaseNow) {
        var checkedAt = Objects.requireNonNull(databaseNow, "databaseNow").atZoneSameInstant(KST);
        if (!isOpen(databaseNow)) {
            throw ConversationException.serviceHoursClosed();
        }
        // 자정부터는 다음 날짜의 00시이므로 다시 시간외가 되며, 잠금 전 날짜를 재사용하지 않는다.
        var date = checkedAt.toLocalDate();
        return new ServiceWindow(date, date.plusDays(1).atStartOfDay(KST).toOffsetDateTime());
    }

    /** 시간 밖에도 종료 정리를 커밋한 뒤 403을 반환할 수 있도록 순수 시간 판정을 제공한다. */
    public static boolean isOpen(OffsetDateTime databaseNow) {
        return !Objects.requireNonNull(databaseNow, "databaseNow").atZoneSameInstant(KST).toLocalTime().isBefore(OPENS_AT);
    }

    /** 동일한 DB 판정 시각에서 계산한 서비스 날짜와 예정 종료 시각이다. */
    public record ServiceWindow(LocalDate serviceDate, OffsetDateTime scheduledEndAt) { }
}
