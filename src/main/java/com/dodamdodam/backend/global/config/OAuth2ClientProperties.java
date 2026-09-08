package com.dodamdodam.backend.global.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.oauth2")
public record OAuth2ClientProperties(
        @NotNull @Valid Credentials google,
        @NotNull @Valid Credentials kakao
) {

    public record Credentials(
            @NotBlank String clientId,
            @NotBlank String clientSecret
    ) {
    }
}
