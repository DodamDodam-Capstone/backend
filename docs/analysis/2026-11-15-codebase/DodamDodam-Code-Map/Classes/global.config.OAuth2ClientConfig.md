---
tags: [backend, code-map, global]
aliases: ["OAuth2ClientConfig"]
---
# OAuth2ClientConfig

`com.dodamdodam.backend.global.config.OAuth2ClientConfig`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:14`
- 작성 메서드/생성자 3개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#clientRegistrationRepository m3ccfcf9a87b4677f\|clientRegistrationRepository]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#google m056c827ec1dc042e\|google]] | 아래 구현과 호출 관계에서 확인 |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#kakao mc0c9909ebaa65b36\|kakao]] | 아래 구현과 호출 관계에서 확인 |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#생성자 m1879c92eebb47f4d|public OAuth2ClientConfig() [자동]]]

## clientRegistrationRepository m3ccfcf9a87b4677f

아래 원본 코드에서 구현을 확인한다.

```java
org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository(com.dodamdodam.backend.global.config.OAuth2ClientProperties properties)
```

설정: `@org.springframework.context.annotation.Bean`

### 이 메서드를 호출하는 곳

- 프레임워크 진입: Spring 컨테이너 Bean 생성

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#google m056c827ec1dc042e|OAuth2ClientConfig.google]] — 호출 · L21
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#kakao mc0c9909ebaa65b36|OAuth2ClientConfig.kakao]] — 호출 · L22
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#google m7640b32867dd7941|OAuth2ClientProperties.google]] — 호출 · L21
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties#kakao m087d0539ef392b14|OAuth2ClientProperties.kakao]] — 호출 · L22

### 외부 라이브러리 호출

- `org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository#<init>(org.springframework.security.oauth2.client.registration.ClientRegistration[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:18`

```java
@Bean
ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
    return new InMemoryClientRegistrationRepository(
            google(properties.google()),
            kakao(properties.kakao())
    );
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## google m056c827ec1dc042e

아래 원본 코드에서 구현을 확인한다.

```java
private org.springframework.security.oauth2.client.registration.ClientRegistration google(com.dodamdodam.backend.global.config.OAuth2ClientProperties.Credentials credentials)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#clientRegistrationRepository m3ccfcf9a87b4677f|OAuth2ClientConfig.clientRegistrationRepository]] — 호출 · L21

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#clientId m6719e57d88fe927c|Credentials.clientId]] — 호출 · L28
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#clientSecret m730c0c58b8b57ef9|Credentials.clientSecret]] — 호출 · L29

### 외부 라이브러리 호출

- `org.springframework.security.config.oauth2.client.CommonOAuth2Provider#getBuilder(java.lang.String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#build()`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#clientId(java.lang.String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#clientSecret(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#scope(java.lang.String @org.jspecify.annotations.Nullable [])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:26`

```java
private ClientRegistration google(OAuth2ClientProperties.Credentials credentials) {
    return CommonOAuth2Provider.GOOGLE.getBuilder("google")
            .clientId(credentials.clientId())
            .clientSecret(credentials.clientSecret())
            .scope("openid", "profile", "email")
            .build();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## kakao mc0c9909ebaa65b36

아래 원본 코드에서 구현을 확인한다.

```java
private org.springframework.security.oauth2.client.registration.ClientRegistration kakao(com.dodamdodam.backend.global.config.OAuth2ClientProperties.Credentials credentials)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientConfig#clientRegistrationRepository m3ccfcf9a87b4677f|OAuth2ClientConfig.clientRegistrationRepository]] — 호출 · L22

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#clientId m6719e57d88fe927c|Credentials.clientId]] — 호출 · L36
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.config.OAuth2ClientProperties.Credentials#clientSecret m730c0c58b8b57ef9|Credentials.clientSecret]] — 호출 · L37

### 외부 라이브러리 호출

- `org.springframework.security.oauth2.client.registration.ClientRegistration#withRegistrationId(java.lang.String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#authorizationUri(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#build()`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#clientAuthenticationMethod(org.springframework.security.oauth2.core.ClientAuthenticationMethod)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#clientId(java.lang.String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#clientName(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#clientSecret(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#issuerUri(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#jwkSetUri(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#redirectUri(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#scope(java.lang.String @org.jspecify.annotations.Nullable [])`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#tokenUri(java.lang.String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#userInfoUri(java.lang.@org.jspecify.annotations.Nullable String)`
- `org.springframework.security.oauth2.client.registration.ClientRegistration.Builder#userNameAttributeName(java.lang.@org.jspecify.annotations.Nullable String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/config/OAuth2ClientConfig.java:34`

```java
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
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m1879c92eebb47f4d

컴파일러가 생성한 멤버다.

```java
public OAuth2ClientConfig()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Object#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
