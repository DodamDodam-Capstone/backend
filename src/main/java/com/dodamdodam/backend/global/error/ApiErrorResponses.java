package com.dodamdodam.backend.global.error;

import com.dodamdodam.backend.global.error.ApiErrorResponse.FieldError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

public final class ApiErrorResponses {

    /** 요청별 상태 없이 사용하는 공통 함수의 인스턴스 생성을 막는다. */
    private ApiErrorResponses() {
    }

    /** 현재 응답의 요청 ID와 검증 오류를 공통 오류 본문에 담는다. */
    public static ApiErrorResponse body(HttpServletResponse response, String code, String message,
                                        List<FieldError> fields) {
        return new ApiErrorResponse(new ApiErrorResponse.ErrorDetail(
                code, message, response.getHeader("X-Request-ID"), fields));
    }

    /** MVC 밖의 오류를 호출자가 결정한 상태 코드와 UTF-8 JSON으로 출력한다. */
    public static void write(JsonMapper mapper, HttpServletResponse response, int status,
                             String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), body(response, code, message, List.of()));
    }
}
