package com.dodamdodam.backend.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2ClientConfig {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        return new InMemoryClientRegistrationRepository(
                google(properties.google()),
                kakao(properties.kakao())
        );
    }

    private ClientRegistration google(OAuth2ClientProperties.Credentials credentials) {
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(credentials.clientId())
                .clientSecret(credentials.clientSecret())
                .scope("openid", "profile", "email")
                .build();
    }

    private ClientRegistration kakao(OAuth2ClientProperties.Credentials credentials) {
        return ClientRegistration.withRegistrationId("kakao")
                .clientId(credentials.clientId())
                .clientSecret(credentials.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile_nickname", "account_email")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v1/oidc/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .jwkSetUri("https://kauth.kakao.com/.well-known/jwks.json")
                .issuerUri("https://kauth.kakao.com")
                .clientName("Kakao")
                .build();
    }
}
