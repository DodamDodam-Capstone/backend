# 전체 참조 자료와 분석 한계

[[analysis/2026-11-15-codebase/DodamDodam-Code-Map/00-Start|처음 보는 담당자용 시작 화면]]

이 자료들은 **누락 없이 추적하기 위한 상세 지도**다. 전체를 한 화면에 맞추면 글자가 작아지므로 먼저 요청별 흐름을 읽고 필요한 구역을 확대한다.

- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/01-Class-Map.canvas|전체 클래스 Canvas]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/02-All-Methods.canvas|전체 메서드 Canvas]]
- [[analysis/2026-11-15-codebase/DodamDodam-Code-Map/Assets/all-class-method-calls.svg|전체 클래스·메서드 SVG]]

타입 106개 · 메서드·생성자 566개 (작성 271 / 자동 295) · 내부 호출 쌍 541개 · 프레임워크 연결 42개.

## 표기와 한계

- 시작 그림: 기능 책임과 큰 흐름. 요청별 그림: 소스에서 확인한 주요 처리 순서로 중간 호출과 분기를 생략한다.
- 전체 호출 지도: 실선은 호출·생성, 점선은 메서드 참조, 파선은 프레임워크 진입이다. 실행 시간순이 아니다.
- 메서드 참조는 즉시 실행을 뜻하지 않는다. lambda 호출은 바깥 소스 메서드에 귀속한다.
- reflection·가상 호출의 모든 구현을 확정하지 않는다. 호출자가 없다고 미사용 코드는 아니다.
- 클래스 노트는 작성 메서드를 먼저 보여주고 생성자·자동 멤버 목록은 접는다. 정확한 정의 링크는 모두 유지한다.
- 원본 코드는 backend/src/main/java에 있다. 노트의 코드 사본을 수정해도 제품 소스는 바뀌지 않는다.
- SVG 클릭은 현재 컴퓨터의 Obsidian 경로를 사용한다. 다른 위치에 clone하면 재생성한다. Canvas·노트는 docs 보관소 상대 경로다.
- 분석은 이 backend의 운영 Java 소스 대상이며 테스트와 의존성 라이브러리의 전체 정의는 포함하지 않는다.

## 재생성

코드가 변경되면 먼저 method-map.json을 다시 분석한다. 생성기는 소스 해시가 다르면 중단한다. 이후 저장소 루트에서 실행한다.

```sh
python3 docs/analysis/2026-11-15-codebase/support/generate_call_vault.py
python3 docs/analysis/2026-11-15-codebase/support/verify_call_vault.py
```

역할과 요청 흐름 설명은 support/call_vault_guide.py에서 관리한다. 재생성 시 메서드 존재는 검사하지만, 코드 의미가 바뀌면 사람이 설명도 함께 검토해야 한다.
커뮤니티 플러그인·외부 서비스 업로드 없이 기본 노트·Canvas로 사용한다.

[JSON Canvas 규격](https://jsoncanvas.org/spec/1.0/) · [Obsidian Canvas](https://obsidian.md/help/plugins/canvas)
