package com.dodamdodam.backend.child.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** API에 공개하는 자녀 프로필이며 계정·세션 식별자는 포함하지 않는다. */
public record ChildView(UUID childId, String name, String nickname, LocalDate birthDate, Gender gender,
                        List<String> interests, String characterId, OffsetDateTime createdAt) {

    public enum Gender { MALE, FEMALE }
}
