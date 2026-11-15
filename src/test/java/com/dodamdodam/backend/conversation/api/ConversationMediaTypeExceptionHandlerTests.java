package com.dodamdodam.backend.conversation.api;

import com.dodamdodam.backend.auth.session.AuthenticatedSessionService;
import com.dodamdodam.backend.auth.session.SessionLifecycleService;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.conversation.application.ConversationService;
import com.dodamdodam.backend.global.error.CommonApiExceptionHandler;
import com.dodamdodam.backend.global.web.ApiResponseHeadersFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConversationMediaTypeExceptionHandlerTests {

    private final AuthenticatedSessionService authentication = mock(AuthenticatedSessionService.class);
    private final ChildService children = mock(ChildService.class);
    private final ConversationService conversations = mock(ConversationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void configureMvcWithoutRequestGuard() {
        // 필터를 통과했다고 가정해 MVC 자체 예외도 공통 500으로 새지 않는지 확인한다.
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ConversationController(authentication, children, conversations), new UnrelatedController())
                .setControllerAdvice(new ConversationMediaTypeExceptionHandler(),
                        new CommonApiExceptionHandler(mock(SessionLifecycleService.class)))
                .addFilters(new ApiResponseHeadersFilter()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/xml", "application/octet-stream"})
    void convertsMvcUnsupportedMediaTypeTo415WithoutInvokingBusinessLogic(String type) throws Exception {
        mockMvc.perform(post("/api/v1/children/33333333-3333-4333-8333-333333333333/conversations")
                        .contentType(type).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.error.fields").isEmpty());
        verifyNoInteractions(authentication, children, conversations);
    }

    @Test
    void doesNotChangeExceptionHandlingForOtherControllers() throws Exception {
        mockMvc.perform(post("/test/unrelated-media-type"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    // 다른 통합 테스트의 컴포넌트 스캔에는 등록되지 않게 격리한다.
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class UnrelatedController {

        @PostMapping("/test/unrelated-media-type")
        public void fail() throws HttpMediaTypeNotSupportedException {
            throw new HttpMediaTypeNotSupportedException("test-only unsupported type");
        }
    }
}
