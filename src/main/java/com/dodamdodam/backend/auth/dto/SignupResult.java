package com.dodamdodam.backend.auth.dto;

import java.util.UUID;

/** 생성된 계정의 공개 식별자만 반환한다. */
public record SignupResult(UUID accountId) { }
