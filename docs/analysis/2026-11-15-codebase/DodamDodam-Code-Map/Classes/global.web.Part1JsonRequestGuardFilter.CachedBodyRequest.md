---
tags: [backend, code-map, global]
aliases: ["CachedBodyRequest"]
---
# CachedBodyRequest

`com.dodamdodam.backend.global.web.Part1JsonRequestGuardFilter.CachedBodyRequest`

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요]] → 이 클래스 · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/90-Reference|전체 참조 자료]]

## 클래스 요약

크기 검사를 마친 본문을 동기 MVC 처리에서 다시 읽을 수 있게 제공한다.

- 원본: `src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:84`
- 작성 메서드/생성자 6개 · 자동 생성 0개

## 메서드 목록

| 메서드 | 하는 일 |
| --- | --- |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getInputStream m5d4ed8ff235514cc\|getInputStream]] | 소비한 원본 스트림 대신 매번 본문 처음부터 읽는 스트림을 반환한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getReader m8ff526dfc87934a9\|getReader]] | 허용한 JSON 인코딩인 UTF-8로 본문을 다시 읽는다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getContentLength ma045bfefef9a0b86\|getContentLength]] | 헤더의 주장 대신 실제 보관한 본문 길이를 제공한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getContentLengthLong mc1798143421ea901\|getContentLengthLong]] | long 길이를 사용하는 소비자에게도 같은 실제 본문 길이를 제공한다. |
| [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getCharacterEncoding meeda2dc107f47521\|getCharacterEncoding]] | Reader와 인코딩 메타데이터를 UTF-8로 일치시킨다. |

> [!example]- 생성자·자동 생성 멤버 1개
>
> - [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#생성자 md064e3f783df1f61|private CachedBodyRequest(HttpServletRequest request, byte[] body)]]

## getInputStream m5d4ed8ff235514cc

소비한 원본 스트림 대신 매번 본문 처음부터 읽는 스트림을 반환한다.

```java
public jakarta.servlet.ServletInputStream getInputStream()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getReader m8ff526dfc87934a9|CachedBodyRequest.getReader]] — 호출 · L134

### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest_anonymous_97#생성자 m6d67966ce14d2441|익명 클래스@97.생성자]] — 생성 · L97

### 외부 라이브러리 호출

- `java.io.ByteArrayInputStream#<init>(byte[])`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:94`

```java
@Override
public ServletInputStream getInputStream() {
    var input = new ByteArrayInputStream(body);
    return new ServletInputStream() {
        /** 보관한 본문에서 한 바이트를 읽는다. */
        @Override
        public int read() {
            return input.read();
        }

        /** JSON 파서의 버퍼 읽기를 보관한 본문으로 위임한다. */
        @Override
        public int read(byte[] bytes, int offset, int length) {
            return input.read(bytes, offset, length);
        }

        /** 해당 스트림의 본문이 모두 소비됐는지 반환한다. */
        @Override
        public boolean isFinished() {
            return input.available() == 0;
        }

        /** 본문이 메모리에 있어 네트워크 입력 대기가 필요하지 않다. */
        @Override
        public boolean isReady() {
            return true;
        }

        /** 현재 동기 MVC 계약에서 비동기 읽기 등록을 허용하지 않는다. */
        @Override
        public void setReadListener(ReadListener listener) {
            Objects.requireNonNull(listener, "readListener");
            throw new IllegalStateException("동기 MVC 본문 읽기만 지원합니다.");
        }
    };
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## getReader m8ff526dfc87934a9

허용한 JSON 인코딩인 UTF-8로 본문을 다시 읽는다.

```java
public java.io.BufferedReader getReader()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter.CachedBodyRequest#getInputStream m5d4ed8ff235514cc|CachedBodyRequest.getInputStream]] — 호출 · L134

### 외부 라이브러리 호출

- `java.io.BufferedReader#<init>(java.io.Reader)`
- `java.io.InputStreamReader#<init>(java.io.InputStream,java.nio.charset.Charset)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:132`

```java
@Override
public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## getContentLength ma045bfefef9a0b86

헤더의 주장 대신 실제 보관한 본문 길이를 제공한다.

```java
public int getContentLength()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:138`

```java
@Override
public int getContentLength() {
    return body.length;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## getContentLengthLong mc1798143421ea901

long 길이를 사용하는 소비자에게도 같은 실제 본문 길이를 제공한다.

```java
public long getContentLengthLong()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:144`

```java
@Override
public long getContentLengthLong() {
    return body.length;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## getCharacterEncoding meeda2dc107f47521

Reader와 인코딩 메타데이터를 UTF-8로 일치시킨다.

```java
public java.lang.String getCharacterEncoding()
```

설정: `@java.lang.Override`

### 이 메서드를 호출하는 곳

명시적 프로젝트 호출 없음. 프레임워크·reflection 전체를 확정한 결과는 아니다.


### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `java.nio.charset.Charset#name()`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:150`

```java
@Override
public String getCharacterEncoding() {
    return StandardCharsets.UTF_8.name();
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]

## 생성자 md064e3f783df1f61

원래 요청의 세션·헤더·속성은 위임하고 검사한 본문만 보관한다.

```java
private CachedBodyRequest(jakarta.servlet.http.HttpServletRequest request, byte[] body)
```

### 이 메서드를 호출하는 곳

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/global.web.Part1JsonRequestGuardFilter#doFilterInternal m647a0f12b7d38584|Part1JsonRequestGuardFilter.doFilterInternal]] — 생성 · L65

### 이 메서드가 호출하는 곳

프로젝트 내부 호출 없음.

### 외부 라이브러리 호출

- `jakarta.servlet.http.HttpServletRequestWrapper#<init>(jakarta.servlet.http.HttpServletRequest)`

### 소스 코드

`src/main/java/com/dodamdodam/backend/global/web/Part1JsonRequestGuardFilter.java:88`

```java
private CachedBodyRequest(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body;
}
```

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/global|기능 개요로 돌아가기]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]]
