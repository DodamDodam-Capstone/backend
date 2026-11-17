package com.dodamdodam.backend.auth.api;

import com.dodamdodam.backend.auth.application.PasswordLoginService;
import com.dodamdodam.backend.auth.dto.AccountView;
import com.dodamdodam.backend.auth.dto.LoginRequest;
import com.dodamdodam.backend.auth.exception.PasswordLoginException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@RestController
public class PasswordLoginController {
    private final PasswordLoginService service;
    private final Validator validator;
    private final JsonMapper requestMapper;

    /** 기존 mapper의 요청 제약과 실제 로그인 서비스를 연결한다. */
    public PasswordLoginController(PasswordLoginService service, Validator validator, JsonMapper mapper) {
        this.service = service;
        this.validator = validator;
        this.requestMapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }

    /** 원문을 프레임워크 인자 로그에 넘기지 않고 유효 UTF-8·구조를 확인한 뒤 로그인한다. */
    @PostMapping("/api/v1/auth/login")
    public Map<String, AccountView> login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LoginRequest input;
        try {
            // guard가 제한한 최대16KiB를 UTF-8로만 해석해 overlong·다른 인코딩 자동 감지를 막는다.
            var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            var text = decoder.decode(ByteBuffer.wrap(request.getInputStream().readAllBytes())).toString();
            input = LoginRequest.fromJson(requestMapper.readTree(text), validator);
        }
        catch (JacksonException | CharacterCodingException invalid) {
            throw PasswordLoginException.validation("body");
        }
        return Map.of("data", service.login(input, request, response));
    }
}
