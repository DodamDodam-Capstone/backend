package com.dodamdodam.backend.guardian.api;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.guardian.application.GuardianPinSetupService;
import com.dodamdodam.backend.guardian.dto.GuardianPinSetupRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@RestController
public class GuardianPinSetupController {
    private final AuthenticatedSessionService authentication;
    private final GuardianPinSetupService service;
    private final JsonMapper mapper;

    /** 중복 키와 뒤따르는 JSON을 거부하는 요청 파서를 구성한다. */
    public GuardianPinSetupController(AuthenticatedSessionService authentication, GuardianPinSetupService service,
                                      JsonMapper mapper) {
        this.authentication = authentication;
        this.service = service;
        this.mapper = mapper.rebuild().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    }

    /** 현재 로그인을 확인한 뒤 PIN을 최초 설정하고 빈 204 응답을 반환한다. */
    @PostMapping("/api/v1/guardian/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setup(HttpServletRequest request) throws IOException {
        var current = authentication.requireAuthenticatedSession(request);
        try {
            var bytes = ByteBuffer.wrap(request.getInputStream().readAllBytes());
            var json = StandardCharsets.UTF_8.newDecoder().decode(bytes).toString();
            service.setup(current, GuardianPinSetupRequest.fromJson(mapper.readTree(json)));
        }
        catch (JacksonException | CharacterCodingException invalid) {
            throw EmailVerificationException.validation("body");
        }
        catch (DataAccessException | TransactionException unavailable) {
            throw new AuthStateUnavailableException(unavailable);
        }
    }
}
