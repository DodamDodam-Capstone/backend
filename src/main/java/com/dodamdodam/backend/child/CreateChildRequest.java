package com.dodamdodam.backend.child;

import java.time.LocalDate;
import java.util.List;

public record CreateChildRequest(String name, String nickname, LocalDate birthDate, ChildView.Gender gender,
                                 List<String> interests, String characterId) {
}
