---
tags: [backend, code-map, child]
aliases: ["CreateChildRequest"]
---
# CreateChildRequest

`com.dodamdodam.backend.child.dto.CreateChildRequest`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

아이 등록 JSON 구조와 값의 검증·정규화를 담당한다.

- 원본: `src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:15`
- 작성 메서드/생성자 5개 · 자동 생성 10개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b\|fromJson]] | JSON 구조와 타입을 검사해 요청을 만들며, 값 정규화는 normalized에서 수행한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1\|normalized]] | 생성자로 직접 만든 요청도 검증하며, 저장에 사용할 정규화된 새 객체를 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#requiredString m56d642f98e92dbce\|requiredString]] | 필수 JSON 문자열을 읽고 누락·null·타입 오류를 해당 필드의 검증 예외로 처리한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalizedText m1fe5e0b24f99439b\|normalizedText]] | 바깥 Unicode 공백을 제거하고 코드포인트 길이와 저장 가능한 문자 여부를 검사한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#isSpace mb94f8bd69ff019a8\|isSpace]] | 양끝 공백 제거에 사용할 Unicode 공백 기준이다. |

> [!example]- 생성자·자동 생성 멤버 10개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#birthDate m521a841c1b562159|public LocalDate birthDate() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#characterId m947b336024310718|public String characterId() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#equals m06e35e4c9eb02043|public final boolean equals(Object o) [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#gender m0bb25172a292801f|public ChildView.Gender gender() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#hashCode mf67e9c5c70288691|public final int hashCode() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#interests m3ec099adee591128|public List<String> interests() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#name mea66a08e5a7112b1|public String name() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#nickname me107b28c9a6d3bad|public String nickname() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#toString m65bd911d24098d6b|public final String toString() [자동]]]
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#생성자 mc86d1916589ca833|public CreateChildRequest(String name, String nickname, LocalDate birthDate, ChildView.Gender gender, List<String> interests, String characterId) [자동]]]

## fromJson mf8c618d868acf95b

JSON 구조와 타입을 검사해 요청을 만들며, 값 정규화는 normalized에서 수행한다.

```java
public static com.dodamdodam.backend.child.dto.CreateChildRequest fromJson(tools.jackson.databind.JsonNode body)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]] — 호출 · L39

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.ChildView.Gender#valueOf mae04a68330eefb58|Gender.valueOf]] — 호출 · L45
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#생성자 mc86d1916589ca833|CreateChildRequest.생성자]] — 생성 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#requiredString m56d642f98e92dbce|CreateChildRequest.requiredString]] — 호출 · L28,29,30,45,58,62
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457|ChildException.validation]] — 호출 · L26,35,40,48,55

### 외부 라이브러리 호출

- `java.time.LocalDate#parse(java.lang.CharSequence)`
- `java.util.ArrayList#<init>()`
- `java.util.ArrayList#add(E)`
- `java.util.List#copyOf(java.util.Collection<? extends E>)`
- `java.util.Set#containsAll(java.util.Collection<?>)`
- `java.util.regex.Matcher#matches()`
- `java.util.regex.Pattern#matcher(java.lang.CharSequence)`
- `tools.jackson.databind.JsonNode#get(int)`
- `tools.jackson.databind.JsonNode#get(java.lang.String)`
- `tools.jackson.databind.JsonNode#isArray()`
- `tools.jackson.databind.JsonNode#isObject()`
- `tools.jackson.databind.JsonNode#propertyNames()`
- `tools.jackson.databind.JsonNode#size()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:23`

```java
public static CreateChildRequest fromJson(JsonNode body) {
    // 세션 계정은 서버에서 결정하므로 accountId 등 정의되지 않은 필드를 거부한다.
    if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
        throw ChildException.validation("body");
    }
    var name = requiredString(body.get("name"), "name");
    var nickname = requiredString(body.get("nickname"), "nickname");
    var date = requiredString(body.get("birthDate"), "birthDate");
    // 날짜 형식과 실제 존재 여부를 모두 검사하며 공백·개행을 보정하지 않는다.
    LocalDate birthDate;
    try {
        if (!DATE.matcher(date).matches()) {
            throw ChildException.validation("birthDate");
        }
        birthDate = LocalDate.parse(date);
    }
    catch (DateTimeParseException invalid) {
        throw ChildException.validation("birthDate");
    }
    // enum 이름과 정확히 일치해야 하므로 대소문자나 바깥 공백을 보정하지 않는다.
    ChildView.Gender gender;
    try {
        gender = ChildView.Gender.valueOf(requiredString(body.get("gender"), "gender"));
    }
    catch (IllegalArgumentException invalid) {
        throw ChildException.validation("gender");
    }
    // 생략은 빈 목록으로 처리하고, 명시적 null과 문자열이 아닌 원소는 거부한다.
    var interests = new ArrayList<String>();
    var supplied = body.get("interests");
    if (supplied != null) {
        if (!supplied.isArray() || supplied.size() > 10) {
            throw ChildException.validation("interests");
        }
        for (int i = 0; i < supplied.size(); i++) {
            interests.add(requiredString(supplied.get(i), "interests"));
        }
    }
    return new CreateChildRequest(name, nickname, birthDate, gender, List.copyOf(interests),
            requiredString(body.get("characterId"), "characterId"));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## normalized mf6b45ea2162d93f1

생성자로 직접 만든 요청도 검증하며, 저장에 사용할 정규화된 새 객체를 반환한다.

```java
public com.dodamdodam.backend.child.dto.CreateChildRequest normalized(java.time.Clock clock, java.util.Set<java.lang.String> supportedCharacterIds)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]] — 호출 · L47

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#생성자 mc86d1916589ca833|CreateChildRequest.생성자]] — 생성 · L95
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalizedText m1fe5e0b24f99439b|CreateChildRequest.normalizedText]] — 호출 · L67,68,83
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457|ChildException.validation]] — 호출 · L71,74,77,85,92

### 외부 라이브러리 호출

- `java.time.Clock#withZone(java.time.ZoneId)`
- `java.time.LocalDate#isAfter(java.time.chrono.ChronoLocalDate)`
- `java.time.LocalDate#now(java.time.Clock)`
- `java.time.ZoneId#of(java.lang.String)`
- `java.util.ArrayList#<init>()`
- `java.util.ArrayList#add(E)`
- `java.util.HashSet#<init>()`
- `java.util.HashSet#add(E)`
- `java.util.List#copyOf(java.util.Collection<? extends E>)`
- `java.util.List#size()`
- `java.util.Set#contains(java.lang.Object)`
- `java.util.regex.Matcher#matches()`
- `java.util.regex.Pattern#matcher(java.lang.CharSequence)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:66`

```java
public CreateChildRequest normalized(Clock clock, Set<String> supportedCharacterIds) {
    var normalizedName = normalizedText(name, 5, "name");
    var normalizedNickname = normalizedText(nickname, 20, "nickname");
    // 서버 기본 시간대와 무관하게 한국 날짜를 기준으로 미래 생일을 거부한다.
    if (birthDate == null || birthDate.isAfter(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))) {
        throw ChildException.validation("birthDate");
    }
    if (gender == null) {
        throw ChildException.validation("gender");
    }
    if (interests == null || interests.size() > 10) {
        throw ChildException.validation("interests");
    }
    // 중복은 공백 제거 후 판정하고, 결과 목록에는 원래 입력 순서를 유지한다.
    var normalizedInterests = new ArrayList<String>();
    var seen = new HashSet<String>();
    for (var interest : interests) {
        var normalized = normalizedText(interest, 30, "interests");
        if (!seen.add(normalized)) {
            throw ChildException.validation("interests");
        }
        normalizedInterests.add(normalized);
    }
    // 캐릭터 ID는 공백 보정 없이 형식과 지원 목록을 모두 만족해야 한다.
    if (characterId == null || !CHARACTER_ID.matcher(characterId).matches()
            || !supportedCharacterIds.contains(characterId)) {
        throw ChildException.validation("characterId");
    }
    // 원본 요청을 유지하고, 정규화한 관심사 목록의 이후 변경도 막는다.
    return new CreateChildRequest(normalizedName, normalizedNickname, birthDate, gender,
            List.copyOf(normalizedInterests), characterId);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## requiredString m56d642f98e92dbce

필수 JSON 문자열을 읽고 누락·null·타입 오류를 해당 필드의 검증 예외로 처리한다.

```java
private static java.lang.String requiredString(tools.jackson.databind.JsonNode node, java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b|CreateChildRequest.fromJson]] — 호출 · L28,29,30,45,58,62

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457|ChildException.validation]] — 호출 · L103

### 외부 라이브러리 호출

- `tools.jackson.databind.JsonNode#isString()`
- `tools.jackson.databind.JsonNode#stringValue()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:100`

```java
private static String requiredString(JsonNode node, String field) {
    // 숫자·불리언 등을 문자열로 자동 변환하지 않아 요청 타입 계약을 지킨다.
    if (node == null || !node.isString()) {
        throw ChildException.validation(field);
    }
    return node.stringValue();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## normalizedText m1fe5e0b24f99439b

바깥 Unicode 공백을 제거하고 코드포인트 길이와 저장 가능한 문자 여부를 검사한다.

```java
private static java.lang.String normalizedText(java.lang.String value, int maximum, java.lang.String field)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1|CreateChildRequest.normalized]] — 호출 · L67,68,83

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#isSpace mb94f8bd69ff019a8|CreateChildRequest.isSpace]] — 호출 · L116,119
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.exception.ChildException#validation m04bce91b4a5ea457|ChildException.validation]] — 호출 · L111,126,131

### 외부 라이브러리 호출

- `java.lang.Character#charCount(int)`
- `java.lang.String#codePointAt(int)`
- `java.lang.String#codePointBefore(int)`
- `java.lang.String#codePointCount(int,int)`
- `java.lang.String#codePoints()`
- `java.lang.String#length()`
- `java.lang.String#substring(int,int)`
- `java.util.stream.IntStream#anyMatch(java.util.function.IntPredicate)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:109`

```java
private static String normalizedText(String value, int maximum, String field) {
    if (value == null) {
        throw ChildException.validation(field);
    }
    // 양끝 경계만 이동해 내부 공백을 보존하고, 코드포인트 단위로 이동해 문자를 분리하지 않는다.
    int start = 0;
    int end = value.length();
    while (start < end && isSpace(value.codePointAt(start))) {
        start += Character.charCount(value.codePointAt(start));
    }
    while (end > start && isSpace(value.codePointBefore(end))) {
        end -= Character.charCount(value.codePointBefore(end));
    }
    var trimmed = value.substring(start, end);
    // 보충 평면 문자도 한 코드포인트로 세어 이름·애칭·관심사의 길이 제한을 적용한다.
    int codePointLength = trimmed.codePointCount(0, trimmed.length());
    if (codePointLength < 1 || codePointLength > maximum) {
        throw ChildException.validation(field);
    }
    // PostgreSQL text의 NUL 오류와 잘못된 surrogate의 UTF-8 변환 손실을 막는다.
    if (trimmed.codePoints().anyMatch(codePoint -> codePoint == 0
            || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE))) {
        throw ChildException.validation(field);
    }
    return trimmed;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isSpace mb94f8bd69ff019a8

양끝 공백 제거에 사용할 Unicode 공백 기준이다.

```java
private static boolean isSpace(int codePoint)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalizedText m1fe5e0b24f99439b|CreateChildRequest.normalizedText]] — 호출 · L116,119

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Character#isSpaceChar(int)`
- `java.lang.Character#isWhitespace(int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/child/dto/CreateChildRequest.java:137`

```java
private static boolean isSpace(int codePoint) {
    // isWhitespace에서 제외되는 NBSP 같은 공백도 포함한다.
    return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## birthDate m521a841c1b562159

컴파일러가 생성한 멤버다.

```java
public java.time.LocalDate birthDate()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L53

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## characterId m947b336024310718

컴파일러가 생성한 멤버다.

```java
public java.lang.String characterId()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L54

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## equals m06e35e4c9eb02043

컴파일러가 생성한 멤버다.

```java
public final boolean equals(java.lang.Object o)
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## gender m0bb25172a292801f

컴파일러가 생성한 멤버다.

```java
public com.dodamdodam.backend.child.dto.ChildView.Gender gender()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L53

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## hashCode mf67e9c5c70288691

컴파일러가 생성한 멤버다.

```java
public final int hashCode()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## interests m3ec099adee591128

컴파일러가 생성한 멤버다.

```java
public java.util.List<java.lang.String> interests()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L54

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## name mea66a08e5a7112b1

컴파일러가 생성한 멤버다.

```java
public java.lang.String name()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L52

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## nickname me107b28c9a6d3bad

컴파일러가 생성한 멤버다.

```java
public java.lang.String nickname()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]] — 호출 · L52

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## toString m65bd911d24098d6b

컴파일러가 생성한 멤버다.

```java
public final java.lang.String toString()
```

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 mc86d1916589ca833

컴파일러가 생성한 멤버다.

```java
public CreateChildRequest(java.lang.String name, java.lang.String nickname, java.time.LocalDate birthDate, com.dodamdodam.backend.child.dto.ChildView.Gender gender, java.util.List<java.lang.String> interests, java.lang.String characterId)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b|CreateChildRequest.fromJson]] — 생성 · L61
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1|CreateChildRequest.normalized]] — 생성 · L95

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.Record#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
