package com.dodamdodam.backend.conversation.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationClosurePropertiesTests {

    private final ApplicationContextRunner context = new ApplicationContextRunner().withUserConfiguration(Binding.class);

    @Test
    void requiresExplicitTimeoutWithoutProductionDefault() {
        context.run(result -> assertThat(result).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s", "1ns", "not-a-duration", ""})
    void rejectsInvalidOrUnrepresentableTimeout(String value) {
        context.withPropertyValues("app.conversation.summary-processing-timeout=" + value)
                .run(result -> assertThat(result).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT1M", "1us"})
    void acceptsExplicitPositivePostgresqlPrecision(String value) {
        context.withPropertyValues("app.conversation.summary-processing-timeout=" + value).run(result -> {
            assertThat(result).hasNotFailed();
            assertThat(result.getBean(ConversationClosureProperties.class).summaryProcessingTimeout()).isPositive();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConversationClosureProperties.class)
    static class Binding { }
}
