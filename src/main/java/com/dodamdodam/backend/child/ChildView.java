package com.dodamdodam.backend.child;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChildView(UUID childId, String name, String nickname, LocalDate birthDate, Gender gender,
                        List<String> interests, String characterId, OffsetDateTime createdAt) {

    public enum Gender { MALE, FEMALE }
}
