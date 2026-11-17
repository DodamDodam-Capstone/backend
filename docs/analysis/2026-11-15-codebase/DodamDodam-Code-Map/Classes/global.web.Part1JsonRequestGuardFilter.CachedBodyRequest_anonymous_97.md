---
tags: [backend, code-map, global]
aliases: ["익명 클래스@97"]
---
# 익명 클래스@97

`com.dodamdodam.backend.global.web.Part1JsonRequestGuardFilter.CachedBodyRequest$anonymous@97`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

보관한 본문에서 한 바이트를 읽는다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:97`
- 작성 메서드/생성자 5개 · 자동 생성 1개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#read m233ad813429f9c62\|read]] | 보관한 본문에서 한 바이트를 읽는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#read m798b24fe4f0fe5cc\|read]] | JSON 파서의 버퍼 읽기를 보관한 본문으로 위임한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#isFinished m6c225c998de800c6\|isFinished]] | 해당 스트림의 본문이 모두 소비됐는지 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#isReady md40c6084942ad245\|isReady]] | 본문이 메모리에 있어 네트워크 입력 대기가 필요하지 않다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#setReadListener m01741868b99a16f9\|setReadListener]] | 현재 동기 MVC 계약에서 비동기 읽기 등록을 허용하지 않는다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#생성자 m6d67966ce14d2441|익명 클래스 [자동]]]

## read m233ad813429f9c62

보관한 본문에서 한 바이트를 읽는다.

```java
public int read()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.io.ByteArrayInputStream#read()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:99`

```java
@Override
public int read() {
    return input.read();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## read m798b24fe4f0fe5cc

JSON 파서의 버퍼 읽기를 보관한 본문으로 위임한다.

```java
public int read(byte[] bytes, int offset, int length)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.io.ByteArrayInputStream#read(byte[],int,int)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:105`

```java
@Override
public int read(byte[] bytes, int offset, int length) {
    return input.read(bytes, offset, length);
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isFinished m6c225c998de800c6

해당 스트림의 본문이 모두 소비됐는지 반환한다.

```java
public boolean isFinished()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.io.ByteArrayInputStream#available()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:111`

```java
@Override
public boolean isFinished() {
    return input.available() == 0;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## isReady md40c6084942ad245

본문이 메모리에 있어 네트워크 입력 대기가 필요하지 않다.

```java
public boolean isReady()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:117`

```java
@Override
public boolean isReady() {
    return true;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## setReadListener m01741868b99a16f9

현재 동기 MVC 계약에서 비동기 읽기 등록을 허용하지 않는다.

```java
public void setReadListener(jakarta.servlet.ReadListener listener)
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.lang.IllegalStateException#<init>(java.lang.String)`
- `java.util.Objects#requireNonNull(T,java.lang.String)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:123`

```java
@Override
public void setReadListener(ReadListener listener) {
    Objects.requireNonNull(listener, "readListener");
    throw new IllegalStateException("동기 MVC 본문 읽기만 지원합니다.");
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 m6d67966ce14d2441

컴파일러가 생성한 멤버다.

```java
익명 클래스@97()
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getInputStream m5d4ed8ff235514cc|CachedBodyRequest.getInputStream]] — 생성 · L97

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.ServletInputStream#<init>()`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
