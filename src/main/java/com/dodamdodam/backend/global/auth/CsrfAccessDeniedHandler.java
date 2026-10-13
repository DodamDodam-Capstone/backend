package com.dodamdodam.backend.global.auth;

import com.dodamdodam.backend.global.error.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
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

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                new ApiErrorResponse.ErrorDetail(
                        "CSRF_INVALID", "CSRF 토큰을 다시 발급받아 주세요.",
                        response.getHeader("X-Request-ID"), List.of()
                )
        ));
    }
}
