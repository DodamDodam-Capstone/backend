package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.application.SignupService;
import com.dodamdodam.backend.auth.dto.SignupRequest;
import com.dodamdodam.backend.auth.dto.SignupResult;
import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.global.error.ApiErrorResponse;
import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@RestController
public class SignupController {
    private final SignupService service;
    private final JsonMapper requestMapper;

    /** 가입 요청은 중복 키와 뒤따르는 JSON까지 검사한다. */
    public SignupController(SignupService service, JsonMapper mapper) {
        this.service = service;
        this.requestMapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }

    /** 토큰·비밀번호 원문을 MVC 인자에서 제외하고 계정 생성 결과만 반환한다. */
    @PostMapping(value = "/api/v1/auth/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, SignupResult>> signup(HttpServletRequest request) throws IOException {
        try {
            var bytes = ByteBuffer.wrap(request.getInputStream().readAllBytes());
            var text = StandardCharsets.UTF_8.newDecoder().decode(bytes).toString();
            var input = SignupRequest.fromJson(requestMapper.readTree(text));
            var result = service.signup(input);
            var location = URI.create("/api/v1/accounts/" + result.accountId());
            return ResponseEntity.created(location).body(Map.of("data", result));
        }
        catch (JacksonException | CharacterCodingException invalid) {
            throw EmailVerificationException.validation("body");
        }
        catch (DataAccessException | TransactionException failure) {
            // 트랜잭션 커밋 중 발생한 장애도 공통 인증 상태 오류로 전달한다.
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 요청 구조·토큰·중복 계정 오류를 공통 JSON 형식으로 응답한다. */
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ApiErrorResponse> handle(EmailVerificationException failure, HttpServletResponse response) {
        return ResponseEntity.status(failure.status()).body(ApiErrorResponses.body(response,
                failure.code(), failure.getMessage(), failure.fields()));
    }
}
