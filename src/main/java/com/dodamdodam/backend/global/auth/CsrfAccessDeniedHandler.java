package com.dodamdodam.backend.global.auth;

import com.dodamdodam.backend.global.error.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class CsrfAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public CsrfAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** CSRF 거부를 기존 고정 메시지와 공통 JSON 형식으로 반환한다. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        ApiErrorResponses.write(jsonMapper, response, HttpServletResponse.SC_FORBIDDEN,
                "CSRF_INVALID", "CSRF 토큰을 다시 발급받아 주세요.");
    }
}
