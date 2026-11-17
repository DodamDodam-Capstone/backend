package com.dodamdodam.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 앱 진입 판단에 필요한 계정·아이·PIN 상태이며 null 필드도 응답에 남긴다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AccountView(UUID accountId,
                          @NotBlank @Email @Size(max = 254) @Pattern(regexp = "[\\x21-\\x7E]+\\z") String email,
                          UUID childId, boolean hasPin,
                          @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
                          OffsetDateTime guardianUnlockedUntil) {

    /** 프레임워크 응답 DEBUG 로그의 문자열 변환에 계정 개인정보를 남기지 않는다. */
    @Override
    public String toString() {
        return "AccountView[redacted]";
    }
}
