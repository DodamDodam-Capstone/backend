package com.dodamdodam.backend.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security")
public record AppSecurityProperties(
        @NotEmpty List<@NotBlank String> allowedOrigins,
        @NotBlank String loginSuccessUrl,
        @NotBlank String logoutSuccessUrl
) {

    public AppSecurityProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
