package com.dodamdodam.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 정상·decoy 모두 같은 길이의 식별자·KST 만료 시각만 공개한다. */
public record VerificationIssueResponse(UUID challengeId,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX") OffsetDateTime expiresAt) { }
