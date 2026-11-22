package com.dodamdodam.backend.guardian.api;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.global.web.ClientAddressResolver;
import com.dodamdodam.backend.guardian.application.GuardianAccessService;
import com.dodamdodam.backend.guardian.application.GuardianUnlockService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@RestController
public class GuardianUnlockController {
    private final AuthenticatedSessionService authentication;
    private final GuardianUnlockService unlock;
    private final GuardianAccessService access;
    private final JsonMapper requestMapper;

    /** 잠금 해제에 필요한 인증, 서비스, 후속 응답 재검사를 연결한다. */
    public GuardianUnlockController(AuthenticatedSessionService authentication, GuardianUnlockService unlock,
                                    GuardianAccessService access, JsonMapper mapper) {
        this.authentication = authentication;
        this.unlock = unlock;
        this.access = access;
        this.requestMapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }

    /** 원문 PIN을 MVC 요청 인자에 노출하지 않고 현재 문맥의 보호자 확인 기한을 반환한다. */
    @PostMapping("/api/v1/guardian/unlock")
    public Map<String, Map<String, String>> unlock(HttpServletRequest request) throws IOException {
        var current = authentication.requireAuthenticatedSession(request);
        unlock.unlock(current, readPin(request), ClientAddressResolver.resolve(request));
        var until = access.requireGuardianAccess(request, current).withOffsetSameInstant(ZoneOffset.ofHours(9));
        return Map.of("data", Map.of("guardianUnlockedUntil", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(until)));
    }

    /** 단일 PIN 문자열의 JSON 구조·UTF-8·ASCII 4자리 계약을 검사한다. */
    private String readPin(HttpServletRequest request) throws IOException {
        try {
            var bytes = ByteBuffer.wrap(request.getInputStream().readAllBytes());
            var text = StandardCharsets.UTF_8.newDecoder().decode(bytes).toString();
            var body = requestMapper.readTree(text);
            if (body == null || !body.isObject() || body.size() != 1 || !body.has("pin")) {
                throw EmailVerificationException.validation("body");
            }
            var value = body.get("pin");
            if (!value.isString() || !value.stringValue().matches("[0-9]{4}")) {
                throw EmailVerificationException.validation("pin");
            }
            return value.stringValue();
        }
        catch (JacksonException | CharacterCodingException invalid) {
            throw EmailVerificationException.validation("body");
        }
    }
}
