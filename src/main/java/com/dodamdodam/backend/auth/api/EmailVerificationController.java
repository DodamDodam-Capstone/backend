package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.application.EmailVerificationIssuanceService;
import com.dodamdodam.backend.auth.application.PasswordResetService;
import com.dodamdodam.backend.auth.dto.*;
import com.dodamdodam.backend.auth.session.*;
import com.dodamdodam.backend.global.web.ClientAddressResolver;
import com.dodamdodam.backend.auth.application.EmailVerificationService;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.io.IOException;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.JacksonException;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;

@RestController
public class EmailVerificationController {
    private final AuthenticatedSessionService authentication;
    private final EmailVerificationIssuanceService issuance;
    private final JsonMapper requestMapper;
    private final EmailVerificationService verification;
    private final PasswordResetService passwordReset;

    /** 조건부 로그인, 인증번호 발급·검증과 비밀번호 재설정을 연결한다. */
    public EmailVerificationController(AuthenticatedSessionService authentication, EmailVerificationIssuanceService issuance,
                                       EmailVerificationService verification, PasswordResetService passwordReset,
                                       JsonMapper mapper) {
        this.authentication = authentication;
        this.issuance = issuance;
        this.verification = verification;
        this.passwordReset = passwordReset;
        this.requestMapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }

    /** PIN 목적은 구조·필드 검사 전에 현재 로그인을 요구하며 허용한 응답 두 필드만 반환한다. */
    @PostMapping("/api/v1/auth/email-verifications")
    public Map<String, VerificationIssueResponse> issue(HttpServletRequest request) throws IOException {
        JsonNode body;
        try {
            // JsonNode로 변환한 뒤에는 사라지는 중복 필드와 뒤따르는 JSON도 입구에서 거부한다.
            // @RequestBody 원문 인자는 framework DEBUG 로그에 남을 수 있어 직접 읽는다.
            body = requestMapper.readTree(request.getReader());
        }
        catch (JacksonException invalid) {
            throw EmailVerificationException.validation("body");
        }
        var purpose = body != null && body.isObject() ? body.get("purpose") : null;
        boolean pin = purpose != null && purpose.isString()
                && (purpose.asText().equals("PIN_SETUP") || purpose.asText().equals("PIN_RESET"));
        var current = pin ? authentication.requireAuthenticatedSession(request) : null;
        var input = IssueEmailVerificationRequest.fromJson(body);
        return Map.of("data", issuance.issue(request, input, current, ClientAddressResolver.resolve(request)));
    }

    /** 인증번호 원문을 MVC 인자에 노출하지 않고 검증 서비스로 넘긴다. */
    @PostMapping(value = "/api/v1/auth/email-verifications/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, VerificationGrantResponse> verify(HttpServletRequest request) throws IOException {
        JsonNode body = readStrictBody(request);
        var id = VerifyEmailCodeRequest.readChallengeId(body);
        return Map.of("data", verification.verify(request, id, () -> VerifyEmailCodeRequest.readCode(body),
            ClientAddressResolver.resolve(request)));
    }

    /** CSRF 및 전송 검사 뒤 재설정을 커밋하고 세션을 생성하지 않는다. */
    @PostMapping("/api/v1/auth/password-resets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(HttpServletRequest request) throws IOException {
        var input = PasswordResetRequest.fromJson(readStrictBody(request));
        try {
            passwordReset.reset(input);
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** guard가 확인한 본문을 UTF-8로 엄격하게 읽고 원문이 MVC 인자 로그에 남지 않게 한다. */
    private JsonNode readStrictBody(HttpServletRequest request) throws IOException {
        try {
            var bytes = ByteBuffer.wrap(request.getInputStream().readAllBytes());
            return requestMapper.readTree(StandardCharsets.UTF_8.newDecoder().decode(bytes).toString());
        }
        catch (JacksonException | CharacterCodingException invalid) {
            throw EmailVerificationException.validation("body");
        }
    }
}
