package com.dodamdodam.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record VerificationGrantResponse(String verificationToken,
                                        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX") OffsetDateTime tokenExpiresAt) {

    /** JSON에는 토큰을 반환하고 MVC 진단 문자열에는 만료 시각만 남긴다. */
    @Override
    public String toString() {
        return "VerificationGrantResponse[tokenExpiresAt=" + tokenExpiresAt + "]";
    }
}
