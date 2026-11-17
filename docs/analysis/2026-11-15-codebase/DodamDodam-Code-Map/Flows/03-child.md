# 아이 등록과 소유권

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|시작]] → [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|기능 개요]] → 이 흐름 → 메서드 정의

**진입:** `POST /api/v1/children`

**결과:** 등록 성공 시 201 · data + Location

> [!important] 먼저 알아둘 점
> 운영 SUPPORTED_CHARACTER_IDS가 비어 있다. 실제 목록을 연결하기 전에는 characterId 검증을 통과하지 못한다.

![[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/03-child.svg]]

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Flows/03-child.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동

번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.

## 단계별 코드

### 1. 로그인 계정 확인

accountId는 세션에서 가져옴 · 본문의 계정 ID를 신뢰하지 않음

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.AuthenticatedSessionService#requireAuthenticatedSession mad23075b25d06a28|AuthenticatedSessionService.requireAuthenticatedSession]]

### 2. JSON 구조 확인

허용 필드와 타입을 검사 · 검증할 요청 객체 생성

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#fromJson mf8c618d868acf95b|CreateChildRequest.fromJson]]

### 3. 값 검증 · 정규화

생년월일·이름·캐릭터 검사 · 업무 트랜잭션 안에서 수행

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#create m022c1fb522a0efea|ChildService.create]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1|CreateChildRequest.normalized]]

### 4. 현재 보안 문맥 잠금

계정 → 문맥 순서로 잠금 · 폐기·버전 변경과 직렬화

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/auth.session.SessionSecurityRepository#requireCurrentContextForUpdate m950184451aa631db|SessionSecurityRepository.requireCurrentContextForUpdate]]

### 5. 아이 저장

children에 등록 · 계정별 등록 제한 처리

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.infrastructure.ChildRepository#insert mcdb7b197a0d8c26e|ChildRepository.insert]]

### 6. 등록 결과 응답

공개 프로필 + 식별 URI · 단건 조회 API 추가는 아님

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildController#create m85a3864c9b94be48|ChildController.create]]

## 실패하거나 달라지는 경우

| 조건 | 결과 / 주의 | 확인할 코드 |
| --- | --- | --- |
| 캐릭터 ID 목록 미확정 | 현재 운영 등록의 선행 조건 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.dto.CreateChildRequest#normalized mf6b45ea2162d93f1\|CreateChildRequest.normalized]] |
| 계정당 등록 제한 | 업무 오류로 반환 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.api.ChildExceptionHandler#handle ma451743715a09223\|ChildExceptionHandler.handle]] |
| 다른 계정의 아이 / 미존재 | 동일한 404로 소유권 정보 보호 | [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Classes/child.application.ChildService#requireChildAccess mbb6af419b1ceab47\|ChildService.requireChildAccess]] |

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|다른 흐름 선택]] · [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Features/child|관련 클래스 찾기]]
