---
tags: [backend, code-map, auth]
aliases: ["InvalidSessionException"]
---
# InvalidSessionException

`com.dodamdodam.backend.auth.session.SessionCookieRenewalFilter.InvalidSessionException`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

MVC가 소비하지 않고 외곽 필터의 본문 초기화·401 변환으로 넘겨야 하는 재검사 실패다.

- 원본: `src/main/java/com/dodamdodam/backend/auth/session/SessionCookieRenewalFilter.java:83`
- 작성 메서드/생성자 0개 · 자동 생성 1개

## 메서드 목록

데이터·설정 타입이다. 아래 목록을 펼쳐 생성자와 접근자를 확인한다.

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter.InvalidSessionException#생성자 ma03397eb8f992755|public InvalidSessionException() [자동]]]

## 생성자 ma03397eb8f992755

컴파일러가 생성한 멤버다.

```java
public InvalidSessionException()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionCookieRenewalFilter#renew me6ea84e6a538169a|SessionCookieRenewalFilter.renew]] — 생성 · L66
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/conversation.api.ConversationStartResponseFilter#doFilterInternal m2e5a11b062f28145|ConversationStartResponseFilter.doFilterInternal]] — 생성 · L106

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.RuntimeException#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/auth|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
