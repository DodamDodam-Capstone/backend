package com.dodamdodam.backend.child;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 내부 자녀 문맥. AI 호칭에는 nickname을 매핑하며 name을 자동 전송하지 않는다. */
public record ChildContext(UUID childId, String name, String nickname, LocalDate birthDate,
                           List<String> interests, String characterId, OffsetDateTime createdAt) {
}
