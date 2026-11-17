package com.dodamdodam.backend.conversation.infrastructure;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class TurnProcessingPropertiesTests {

    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(Binding.class);

    @Test
    void requiresExplicitTimeoutWithoutProductionDefault() {
        context.run(result -> assertThat(result).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s", "1ns", "not-a-duration", ""})
    void rejectsInvalidOrUnrepresentableTimeout(String value) {
        context.withPropertyValues("app.conversation.turn.processing-timeout=" + value)
                .run(result -> assertThat(result).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT1M", "1us"})
    void acceptsExplicitPositivePostgresqlPrecision(String value) {
        context.withPropertyValues("app.conversation.turn.processing-timeout=" + value).run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result.getBean(TurnProcessingProperties.class).processingTimeout()).isPositive();
        });
    }

    @Test
    void bindsTurnAndSummaryTimeoutsIndependently() {
        // 두 설정을 함께 등록해 발화·요약 기한이 각각의 경로에서 바인딩되는지 확인한다.
        new ApplicationContextRunner().withUserConfiguration(CombinedBinding.class).withPropertyValues(
                "app.conversation.summary-processing-timeout=2s",
                "app.conversation.turn.processing-timeout=3s").run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result.getBean(ConversationClosureProperties.class).summaryProcessingTimeout()).isEqualTo(Duration.ofSeconds(2));
            assertThat(result.getBean(TurnProcessingProperties.class).processingTimeout()).isEqualTo(Duration.ofSeconds(3));
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(TurnProcessingProperties.class)
    static class Binding { }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({ConversationClosureProperties.class, TurnProcessingProperties.class})
    static class CombinedBinding { }
}
