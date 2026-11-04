package com.dodamdodam.backend.auth.session;

import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class AuthStateErrorHandler implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public AuthStateErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** 인증이 필요한 요청에 기존 401 오류를 반환한다. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        ApiErrorResponses.write(jsonMapper, response, 401, "AUTHENTICATION_REQUIRED", "로그인이 필요합니다.");
    }

    /** 인증 상태를 확인할 수 없을 때 기존 503 오류를 반환한다. */
    public void unavailable(HttpServletResponse response) throws IOException {
        ApiErrorResponses.write(jsonMapper, response, 503,
                "AUTH_STATE_UNAVAILABLE", "인증 상태를 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");
    }
}
