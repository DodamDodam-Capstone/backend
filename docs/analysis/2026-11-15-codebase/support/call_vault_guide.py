"""처음 보는 팀원을 위한 역할 요약과 짧은 요청 흐름을 같은 데이터로 그린다."""
from html import escape
from urllib.parse import urlencode
import unicodedata


# 요약은 사람의 읽는 순서이며, 완전한 정적 호출 그래프와 구분한다.
FEATURE_INFO = {
    'global': ('공통 요청 · 오류', '요청을 받기 전 검사하고 같은 오류 형식으로 응답한다.',
               'CORS · CSRF · 요청 제한', 'JSON 형식 · 크기 · 오류 응답', '구현됨 · 공통 진입 경계',
               ['01-request'], ['SecurityConfig.applicationSecurityFilterChain', 'GlobalApiRateLimitFilter.doFilterInternal', 'ApiErrorResponses.write']),
    'auth': ('인증 · 이메일', '현재 세션 권한을 확인하고 이메일 인증번호를 발급한다.',
             '로그인 문맥 · 로그아웃', '인증번호 발급 · SMTP 전송', '부분 구현 · 가입 / OAuth 연동 후속',
             ['02-email', '05-session'], ['EmailVerificationController.issue', 'AuthenticatedSessionService.requireAuthenticatedSession', 'AuthLogoutController.logout']),
    'child': ('아이 등록 · 소유권', '로그인 계정에 아이를 등록하고 해당 아이의 소유자인지 확인한다.',
              '아이 등록 · 입력 검증', '다른 계정의 아이 접근 차단', '선행 조건 · 실제 캐릭터 목록 미정',
              ['03-child'], ['ChildController.create', 'ChildService.create', 'ChildService.requireChildAccess']),
    'conversation': ('대화 시작 · 상태', '아이의 대화를 시작하고 중복 시작과 종료 상태를 조정한다.',
                     '신규 시작 · 재전송 구분', 'ACTIVE → CLOSING → ENDED', '부분 구현 · 음성 / 종료 API 후속',
                     ['04-conversation', '07-closure'], ['ConversationController.start', 'ConversationService.create', 'ConversationClosureCoordinator.reconcile']),
    'guardian': ('보호자 권한', '현재 로그인 문맥의 보호자 확인 기한과 PIN 버전을 검증한다.',
                 'PIN 버전 · 보호자 확인 기한', '응답 직전 권한 재검사', '기반 구현 · PIN / 기록 API 후속',
                 ['06-guardian'], ['GuardianAccessService.requireGuardianAccess', 'GuardianAccessService.verifyResponseAccess', 'GuardianAccessRepository.findState']),
    'app': ('실행 · 저장 · 외부 연결', 'Spring을 시작하고 DB·SMTP·후속 AI 연결의 경계를 확인한다.',
            'PostgreSQL · Flyway · SMTP', 'STT / TTS 연결은 후속 구현', '현재 연결과 후속 연결을 구분',
            ['01-request'], ['DodamDodamBackendApplication.main']),
}

ROLES = {
    'ChildController': '아이 등록 HTTP 진입점. 세션의 계정으로 등록하고 201 응답을 만든다.',
    'ChildService': '아이 입력 검증·등록과 소유권 검사를 수행한다. 등록 시 현재 보안 문맥을 잠근다.',
    'ChildRepository': 'children 테이블의 등록·소유권 조회·잠금 조회를 담당한다.',
    'CreateChildRequest': '아이 등록 JSON 구조와 값의 검증·정규화를 담당한다.',
    'ChildView': '아이의 공개 프로필을 반환하는 응답 데이터다.',
    'ChildContext': '인증과 소유권 검사가 끝난 뒤 내부 처리에 전달하는 아이 정보다.',
    'EmailVerificationController': '이메일 발급 HTTP 진입점. JSON을 읽고 PIN 목적의 선행 인증을 처리한다.',
    'EmailVerificationIssuanceService': '요청 예산 → challenge 저장 → SMTP → 응답 권한 재검사 순서를 조정한다.',
    'EmailVerificationDeliveryService': '업무 트랜잭션 밖에서 메일을 보내고 전송 실패 시 해당 발급을 폐기한다.',
    'EmailVerificationRepository': '이메일 challenge의 잠금·재발급·검증 기반 상태를 저장한다.',
    'EmailVerificationAccountRepository': '발급 적격 여부와 PIN 목적에 필요한 현재 계정·PIN 상태를 조회한다.',
    'SmtpEmailVerificationSender': 'SMTP 서버에 실제 메일 또는 계정 노출 방지용 안내 메일을 전달한다.',
    'EmailVerificationKeys': '인증번호와 요청 제한에 필요한 전용 HMAC 키를 읽고 검증한다.',
    'EmailVerificationCodeGenerator': '보안 난수로 정확히 6자리 인증번호를 생성한다.',
    'AuthRateLimitService': '독립 트랜잭션으로 요청 제한 예산을 예약한다.',
    'AuthRateLimitRepository': '요청 제한 키를 잠그고 시간 창별 사용량을 저장한다.',
    'AuthenticatedSession': '검증한 계정 ID와 업무 보안 문맥 ID를 전달한다. 객체 자체가 현재 권한을 보증하지 않는다.',
    'AuthenticatedSessionService': '프레임워크 인증·저장된 세션·현재 DB 보안 문맥이 일치하는지 검증한다.',
    'SessionSecurityRepository': '업무 로그인 문맥 생성·유효성 확인·잠금·폐기를 담당한다.',
    'SessionLifecycleService': '로그인 시 새 업무 문맥과 네이티브 세션을 연결하고 실패를 보상한다.',
    'AuthStateErrorFilter': 'MVC 밖의 인증 장애를 처리하고 성공 응답을 보내기 직전 권한을 재검사한다.',
    'SessionCookieRenewalFilter': '현재 세션의 유효성을 다시 확인한 뒤 로그인 쿠키를 갱신한다.',
    'AuthLogoutController': '현재 업무 문맥을 폐기하고 네이티브 세션·CSRF 정보를 정리한다.',
    'GuardianAccessService': '로그인 문맥·보호자 확인 기한·PIN 버전을 확인하고 응답 재검사를 연결한다.',
    'GuardianAccessRepository': 'DB 시각을 기준으로 현재 문맥과 보호자 권한 상태를 함께 조회한다.',
    'ConversationController': '대화 시작 요청을 받아 인증·입력·소유권을 검사하고 201 또는 200을 반환한다.',
    'ConversationService': '잠금 아래 신규 시작·같은 키 재전송·충돌을 판단하고 응답 대상도 재검사한다.',
    'ConversationRepository': '대화 시작·중복 조회·세션 연결과 관련된 SQL 및 잠금을 담당한다.',
    'ConversationClosureCoordinator': '진행 중 발화 정리를 기다리며 대화를 ACTIVE → CLOSING → ENDED로 조정한다.',
    'ConversationClosureRepository': '종료 경계·발화 상태·요약 대기 상태·기존 연결의 복구 기한을 저장한다.',
    'ConversationStartResponseFilter': '대화 시작의 성공 응답 직전에 현재 접근 권한과 대화 상태를 다시 확인한다.',
    'ConversationServiceHours': '대화 서비스 이용시간과 서비스 날짜 경계를 계산·검증한다.',
    'SecurityConfig': 'CORS·CSRF·OAuth와 사용자 정의 필터를 Spring Security 체인에 연결한다.',
    'GlobalApiRateLimitFilter': 'API·OAuth 요청의 IP 예산을 업무 처리 전에 예약하고 초과 요청을 거부한다.',
    'Part1JsonRequestGuardFilter': '로그아웃·아이 등록·이메일 발급 요청의 본문 크기와 JSON 전송 형식을 검사한다.',
    'Part2JsonRequestGuardFilter': 'Part 2 요청의 JSON 전송 조건을 보안 체인 안에서 검사한다.',
    'ApiResponseHeadersFilter': '공통 요청 식별자와 응답 캐시 정책 헤더를 설정한다.',
    'ApiErrorResponses': '공통 오류 본문을 만들고 필터 등 MVC 밖에서도 JSON 오류를 쓴다.',
    'CommonApiExceptionHandler': '기능별 처리기가 다루지 않은 MVC 오류를 공통 응답으로 변환한다.',
    'CsrfController': '클라이언트가 변경 요청에 사용할 CSRF 토큰 정보를 반환한다.',
    'DodamDodamBackendApplication': 'Spring Boot 애플리케이션을 시작하는 main 진입점이다.',
}

# 각 단계의 화살표는 주요 처리 순서다. 직접 호출 여부는 메서드 노트에서 확인한다.
FLOWS = [
    dict(id='01-request', title='요청은 어디서 검사되는가', feature='global',
         trigger='HTTP 요청 → 공통 필터 → 보안 체인 → MVC',
         result='요청이 검사를 통과하면 Controller로 전달한다. 실패는 해당 경계에서 중단한다.',
         caution='필터 화살표는 실행 경계의 요약이다. 필터끼리 직접 Java 메서드를 호출한다는 뜻은 아니다.',
         steps=[
             ('요청 추적 · 응답 보호', 'requestId와 no-store 설정\n인증 오류 처리 경계 구성', ['ApiResponseHeadersFilter.doFilterInternal', 'AuthStateErrorFilter.doFilterInternal']),
             ('세션 · 보안 체인', '세션 쿠키 갱신 경계\nSpring Session 뒤 CORS 처리', ['SessionCookieRenewalFilter.doFilterInternal', 'SecurityConfig.applicationSecurityFilterChain']),
             ('전역 요청 제한', 'API / OAuth IP 예산 예약\n과도한 요청은 여기서 중단', ['GlobalApiRateLimitFilter.doFilterInternal']),
             ('본문 전송 검사', '해당 경로의 크기·형식 검사\nPart 2 → Part 1 필터 순서', ['Part2JsonRequestGuardFilter.doFilterInternal', 'Part1JsonRequestGuardFilter.doFilterInternal']),
             ('CSRF · 인증 · 업무 처리', 'Spring Security 검사 후 MVC\n예시: 아이 등록 Controller', ['SecurityConfig.applicationSecurityFilterChain', 'ChildController.create']),
             ('응답 직전 재검사', '대상 요청의 최신 권한 확인\n실패한 성공 본문·쿠키 제거', ['AuthStateErrorFilter.verifyResponseAccess', 'ConversationStartResponseFilter.verify']),
         ],
         failures=[('요청 제한 초과', '429와 재시도 기한', 'GlobalApiRateLimitFilter.doFilterInternal'), ('권한 확인 저장소 장애', '인증 상태 확인 불가 503', 'AuthStateErrorFilter.doFilterInternal'), ('MVC 처리 오류', '기능별 처리 후 공통 오류 처리', 'CommonApiExceptionHandler.handle')]),
    dict(id='02-email', title='이메일 인증번호 발급', feature='auth',
         trigger='POST /api/v1/auth/email-verifications', result='200 · data.challengeId / expiresAt',
         caution='challenge를 커밋한 뒤 SMTP를 호출한다. 발급 API이며 인증번호 검증·회원가입 API는 후속이다.',
         steps=[
             ('요청 · 목적 검사', 'JSON을 읽고 허용 필드 확인\nPIN 목적은 로그인 먼저 확인', ['EmailVerificationController.issue']),
             ('정규화 · 예산 예약', '이메일·목적별 제한 예약\n6자리 번호 생성과 HMAC', ['EmailVerificationIssuanceService.issue', 'AuthRateLimitService.reserve']),
             ('목적별 현재 상태 확인', '공개 목적: 계정 적격 여부\nPIN 목적: 현재 문맥·PIN 잠금', ['EmailVerificationIssuanceService.preparePublic', 'EmailVerificationIssuanceService.preparePin']),
             ('challenge 저장 · 커밋', '이전 증명을 무효화\n새 발급과 만료 시각 저장', ['EmailVerificationRepository.replace']),
             ('메일 전달', '업무 트랜잭션 밖에서 SMTP\n실패하면 이 발급만 폐기', ['EmailVerificationDeliveryService.deliver', 'SmtpEmailVerificationSender.send']),
             ('응답 권한 재확인', '현재 proof·문맥·PIN 재검사\n보낼 때도 최신 상태 확인', ['EmailVerificationIssuanceService.verifyResponseAccess', 'AuthStateErrorFilter.verifyResponseAccess']),
         ],
         failures=[('발급 예산 / 재전송 제한', '429 · Retry-After', 'EmailVerificationExceptionHandler.limited'), ('SMTP 실패 / 응답 유실', '해당 발급을 보상 폐기 후 오류 반환', 'EmailVerificationDeliveryService.invalidate'), ('전송 사이 권한 변경', '해당 발급 폐기·성공 응답 거부', 'EmailVerificationIssuanceService.verifyResponseAccess')]),
    dict(id='03-child', title='아이 등록과 소유권', feature='child',
         trigger='POST /api/v1/children', result='등록 성공 시 201 · data + Location',
         caution='운영 SUPPORTED_CHARACTER_IDS가 비어 있다. 실제 목록을 연결하기 전에는 characterId 검증을 통과하지 못한다.',
         steps=[
             ('로그인 계정 확인', 'accountId는 세션에서 가져옴\n본문의 계정 ID를 신뢰하지 않음', ['ChildController.create', 'AuthenticatedSessionService.requireAuthenticatedSession']),
             ('JSON 구조 확인', '허용 필드와 타입을 검사\n검증할 요청 객체 생성', ['CreateChildRequest.fromJson']),
             ('값 검증 · 정규화', '생년월일·이름·캐릭터 검사\n업무 트랜잭션 안에서 수행', ['ChildService.create', 'CreateChildRequest.normalized']),
             ('현재 보안 문맥 잠금', '계정 → 문맥 순서로 잠금\n폐기·버전 변경과 직렬화', ['SessionSecurityRepository.requireCurrentContextForUpdate']),
             ('아이 저장', 'children에 등록\n계정별 등록 제한 처리', ['ChildRepository.insert']),
             ('등록 결과 응답', '공개 프로필 + 식별 URI\n단건 조회 API 추가는 아님', ['ChildController.create']),
         ],
         failures=[('캐릭터 ID 목록 미확정', '현재 운영 등록의 선행 조건', 'CreateChildRequest.normalized'), ('계정당 등록 제한', '업무 오류로 반환', 'ChildExceptionHandler.handle'), ('다른 계정의 아이 / 미존재', '동일한 404로 소유권 정보 보호', 'ChildService.requireChildAccess')]),
    dict(id='04-conversation', title='대화 시작과 재전송', feature='conversation',
         trigger='POST /api/v1/children/{childId}/conversations', result='신규 201 + Location · 허용된 같은 키 재전송 200',
         caution='clientRequestId 재전송은 현재 연결된 당일 ACTIVE 대화만 허용한다. 응답 재검사에서는 새 연결을 만들지 않는다.',
         steps=[
             ('인증 · 입력 · 소유권', '로그인 → UUID / JSON 검사\n해당 아이의 소유권 확인', ['ConversationController.prepareStart']),
             ('상위 데이터 잠금', '계정 → 문맥 → 아이 → 대화\n대기 후 DB 시각 재확인', ['ConversationService.create', 'ConversationService.decideStart']),
             ('이전 대화 · 시간 판단', '필요하면 기존 종료 상태 정리\n이용시간과 시작 키를 판단', ['ConversationService.decideStart', 'ConversationClosureCoordinator.reconcile']),
             ('신규 또는 재전송', '신규: 대화와 세션 연결 저장\n같은 키: 기존 연결 재검사', ['ConversationRepository.insertActive', 'ConversationService.replay']),
             ('응답 대상 기록', '검증한 대화 ID를 요청에 기록\n신규 / 재전송 응답 구분', ['ConversationController.start', 'ConversationStartResponseFilter.protect']),
             ('현재 상태 다시 확인', '응답 직전 권한·시간·상태 확인\n실패하면 성공 응답을 비움', ['ConversationStartResponseFilter.verify', 'ConversationService.verifyStartResponse']),
         ],
         failures=[('소유하지 않은 아이', '404 · 미존재와 동일', 'ChildService.requireChildAccess'), ('미종료 대화 / 종료된 키 충돌', '409 · 업무 상태별 분류', 'ConversationService.conflictFor'), ('응답 전 종료 / 권한 변경', '기존 성공 본문·Location 제거', 'ConversationStartResponseFilter.clearSuccess')]),
    dict(id='05-session', title='로그인 문맥 검증과 로그아웃', feature='auth',
         trigger='로그인이 필요한 요청 · POST /api/v1/auth/logout', result='현재 계정·문맥 반환 · 로그아웃 성공은 204',
         caution='1~3단계는 인증 공통 경로다. 4~6단계는 로그아웃 요청에서만 수행하며 로그인·OAuth 계정 연동 전체 흐름은 아니다.',
         steps=[
             ('현재 인증 요청', '실제 요청의 세션을 확인\n없거나 무효면 인증 거부', ['AuthenticatedSessionService.requireAuthenticatedSession']),
             ('저장된 정보 대조', 'Authentication과 저장 세션\nSecurityContext 일치 검사', ['AuthenticatedSessionService.validate']),
             ('업무 문맥 검증', '계정·세션 버전·폐기·만료\nDB의 현재 상태로 판단', ['SessionSecurityRepository.isCurrentContext']),
             ('로그아웃 요청 검사', '인증 성공과 빈 본문 확인\n이후는 로그아웃 전용 경로', ['AuthLogoutController.logout']),
             ('업무 문맥 폐기', '현재 문맥과 PIN 증명 정리\n뒤이은 요청에서 사용 불가', ['SessionSecurityRepository.revokeCurrentContext']),
             ('세션 · CSRF 정리', 'Spring 로그아웃 핸들러 호출\n완료 시 204 응답', ['AuthLogoutController.logout']),
         ],
         failures=[('문맥 만료 / 폐기 / 불일치', '401 · 인증 필요', 'AuthenticatedSessionService.validate'), ('인증 저장소 확인 실패', '503 · 미로그인과 구별', 'AuthenticatedSessionService.requireAuthenticatedSession'), ('로그아웃 본문이 비어 있지 않음', '400 · 입력 오류', 'AuthLogoutController.logout')]),
    dict(id='06-guardian', title='보호자 권한 확인', feature='guardian',
         trigger='인증·아이 소유권 검사를 마친 내부 호출자', result='유효한 guardianUntil 반환 · 요청에 재검사 대상 기록',
         caution='공통 검증 서비스의 사용 계약이다. 이를 호출하는 PIN 설정·해제·기록 HTTP API까지 구현됐다는 뜻은 아니다.',
         steps=[
             ('보호자 권한 요구', '호출자가 인증·소유권 선행\n현재 계정과 문맥 전달', ['GuardianAccessService.requireGuardianAccess']),
             ('현재 상태 함께 조회', '문맥·계정·PIN 버전 조회\nDB 시각을 기준으로 검사', ['GuardianAccessRepository.findState']),
             ('로그인 문맥 검사', '세션 버전·폐기·만료 확인\n무효면 인증 필요 오류', ['GuardianAccessState.isSessionCurrent']),
             ('보호자 확인 검사', 'guardianUntil과 PIN 버전\n현재 권한인지 함께 확인', ['GuardianAccessState.isGuardianCurrent']),
             ('재검사 대상 기록', '성공한 계정·문맥만 보관\n권한 기한 자체는 연장 안 함', ['GuardianAccessService.requireGuardianAccess']),
             ('응답 전 다시 조회', 'AuthStateErrorFilter에서 호출\n현재 DB 권한으로 재확인', ['GuardianAccessService.verifyResponseAccess', 'AuthStateErrorFilter.verifyResponseAccess']),
         ],
         failures=[('로그인 문맥 무효', '401 · 인증 필요', 'GuardianAccessService.requireCurrentAccess'), ('보호자 확인 만료 / PIN 버전 변경', '403 · 보호자 확인 필요', 'GuardianAccessService.requireCurrentAccess'), ('전송 전 권한 상실', '성공 응답을 현재 오류로 교체', 'GuardianAccessExceptionHandler.handle')]),
    dict(id='07-closure', title='대화 종료 상태 조정', feature='conversation',
         trigger='ConversationClosureCoordinator.reconcile 내부 호출', result='CLOSING 유지 또는 ENDED 전환 · 기존 종료 상태 반환',
         caution='타이머·수동 종료 HTTP API·AI 요약 worker 자체는 아니다. PENDING 기록이 요약 실행을 의미하지 않는다.',
         steps=[
             ('상위 잠금 · 상태 조회', '인증·연결 검사는 호출자 책임\n아이·대화 관계와 상태 조회', ['ConversationClosureCoordinator.reconcile']),
             ('종료 경계 확정', '이미 ENDED면 기존 상태 반환\n최초 종료 경계만 기록', ['ConversationClosureRepository.beginClosing']),
             ('처리 중 발화 정리', 'PROCESSING 발화를 잠금\n만료한 처리 상태를 정리', ['ConversationClosureRepository.lockProcessingTurns', 'ConversationClosureRepository.expireProcessing']),
             ('대기 또는 종료 진행', 'PROCESSING이 남으면 CLOSING\n없으면 기존 연결을 잠금', ['ConversationClosureCoordinator.reconcile', 'ConversationClosureRepository.lockLinks']),
             ('ENDED · 요약 상태 저장', '요약 입력 유무를 확인\nEMPTY 또는 PENDING 기록', ['ConversationClosureRepository.finishClosing']),
             ('기존 연결 복구 기한', '적격 연결에 600초 기한\n새 연결을 만들지는 않음', ['ConversationClosureRepository.grantRecovery']),
         ],
         failures=[('대화가 없거나 아이 불일치', '404 · 같은 오류로 처리', 'ConversationClosureCoordinator.requireState'), ('PROCESSING이 아직 남아 있음', '실패가 아니라 CLOSING 대기', 'ConversationClosureCoordinator.reconcile'), ('이미 ENDED인 재호출', '기존 상태 반환', 'ConversationClosureCoordinator.reconcile')]),
]


def resolve(g, reference):
    """짧은 설명용 이름을 실제 분석된 유일한 메서드로 해석하며 모호하면 중단한다."""
    owner, name = reference.rsplit('.', 1)
    matches = [mid for mid, m in g.METHODS.items()
               if g.CLASSES[m['owner']]['name'] == owner and m['name'] == name and not m['generated']]
    assert len(matches) == 1, (reference, matches)
    return matches[0]


def class_summary(c, methods):
    """역할 설명이 없는 타입은 계층·데이터 성격과 실제 메서드 설명을 요약한다."""
    if c['name'] in ROLES:
        return ROLES[c['name']]
    if c['doc']:
        return c['doc'].strip()
    descriptions = [m['doc'].strip() for m in methods if m.get('doc') and m['name'] != '<init>' and not m['generated']]
    if c['kind'] in ('RECORD', 'ENUM'):
        return ('값을 전달하는 데이터 타입이다.' if c['kind'] == 'RECORD' else '허용 상태·종류를 정의하는 열거형이다.')
    if 'ExceptionHandler' in c['name']:
        return descriptions[0] if descriptions else '기능 오류를 HTTP 응답으로 변환한다.'
    if 'Exception' in c['name']:
        return '업무 실패의 상태 코드와 공개 오류 정보를 전달한다.'
    return descriptions[0] if descriptions else '프레임워크 설정 또는 보조 타입이다. 아래 메서드와 소스에서 연결 위치를 확인한다.'


def wrap_line(value, units=40):
    """한글 폭을 고려해 SVG 카드의 글자가 경계를 넘지 않게 줄바꿈한다."""
    lines, line, size = [], '', 0
    for char in value:
        width = 2 if unicodedata.east_asian_width(char) in 'WF' else 1
        if size + width > units:
            lines.append(line.rstrip()); line, size = '', 0
        line += char; size += width
    return lines + [line.rstrip()]


def note_uri(g, target):
    """SVG의 카드를 기존 docs 보관소의 해당 노트로 연결한다."""
    return 'obsidian://open?' + urlencode({'path': str(g.VAULT / target), 'paneType': 'tab'})


def diagram(g, stem, title, subtitle, cards, edges, width, height, footer):
    """같은 카드 위치로 native Canvas와 읽기 쉬운 독립 SVG를 함께 만든다."""
    palette = {'5': '#2555a6', '4': '#146244', '3': '#855400', '6': '#624094'}
    nodes = [{'id': 'heading', 'type': 'text', 'x': 40, 'y': 20, 'width': width-80,
              'height': 100, 'text': '# '+title+'\n\n'+subtitle}]
    canvas_edges, svg = [], [f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title desc">',
                            '<title id="title">'+escape(title)+'</title><desc id="desc">'+escape(subtitle)+'</desc>',
                            '<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#8a99ad"/></marker></defs>',
                            f'<rect width="{width}" height="{height}" rx="20" fill="#f6f8fc"/>',
                            '<g font-family="Apple SD Gothic Neo, Noto Sans CJK KR, Arial, sans-serif" fill="#17253b">']
    def text(x, y, value, size=20, fill='#17253b', weight='400'):
        """레이아웃 좌표와 원문 텍스트를 XML 이스케이프해 기록한다."""
        svg.append(f'<text x="{x}" y="{y}" font-size="{size}" fill="{fill}" font-weight="{weight}">{escape(value)}</text>')
    text(40, 58, title, 32, weight='700'); text(40, 96, subtitle, 19, '#53647b')
    by_id = {card['id']: card for card in cards}
    for index, (source, target, from_side, to_side) in enumerate(edges):
        a, b = by_id[source], by_id[target]
        def point(card, side):
            """변의 중앙을 연결해 선이 카드 글자를 관통하지 않게 한다."""
            return {'right': (card['x']+card['w'], card['y']+card['h']/2), 'left': (card['x'],card['y']+card['h']/2),
                    'bottom': (card['x']+card['w']/2,card['y']+card['h']), 'top': (card['x']+card['w']/2,card['y'])}[side]
        x1, y1 = point(a, from_side); x2, y2 = point(b, to_side)
        svg.append(f'<path d="M{x1} {y1} L{x2} {y2}" stroke="#8a99ad" stroke-width="2" marker-end="url(#arrow)" fill="none"/>')
        canvas_edges.append({'id': 'flow-'+str(index), 'fromNode': source, 'fromSide': from_side, 'toNode': target, 'toSide': to_side, 'toEnd': 'arrow', 'color': '5'})
    for card in cards:
        x,y,w,h=card['x'],card['y'],card['w'],card['h']
        accent = palette[card.get('color', '5')]
        links = card.get('links', [])
        nodes.append({'id':card['id'], 'type':'text', 'x':x, 'y':y, 'width':w, 'height':h, 'color':card.get('color','5'),
                      'text':'### '+card['title']+'\n\n'+card['body'].replace('\n','\n\n')+('\n\n'+' · '.join(links) if links else '')})
        if card.get('uri'):svg.append('<a href="'+escape(card['uri'],quote=True)+'">')
        svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="white" stroke="#d5deea"/>')
        svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="5" rx="2" fill="{accent}"/>')
        text(x+22,y+40,card['title'],22,accent,'700')
        line_y = y+77
        for paragraph in card['body'].split('\n'):
            for line in wrap_line(paragraph, int((w-44)/10)):
                text(x+22,line_y,line,20);line_y+=28
        assert line_y <= y+h-35, (stem,card['id'],'본문 높이 초과')
        if card.get('hint'):text(x+22,y+h-21,card['hint'],17,accent)
        if card.get('uri'):svg.append('</a>')
    nodes.append({'id':'legend','type':'text','x':40,'y':height-70,'width':width-80,'height':55,'text':footer})
    text(40,height-40,footer,17,'#53647b')
    svg.append('</g></svg>')
    g.save(stem+'.canvas', {'nodes': nodes, 'edges': canvas_edges})
    g.save('Assets/'+stem.split('/')[-1]+'.svg', '\n'.join(svg))


def write_guide(g):
    """역할 개요 → 요청별 흐름 → 메서드 정의 순서로 탐색 자료를 만든다."""
    for flow in FLOWS:
        for _, _, refs in flow['steps']:
            for ref in refs:resolve(g,ref)
        for _,_,ref in flow['failures']:resolve(g,ref)
    for info in FEATURE_INFO.values():
        for ref in info[6]:resolve(g,ref)
    write_overview(g)
    for flow in FLOWS:write_flow(g,flow)
    write_feature_notes(g)
    write_start(g)


def write_overview(g):
    """읽기 보기의 본문 폭에서도 기능 6개의 글자를 읽을 수 있게 배치한다."""
    cards=[]
    for i,f in enumerate(['global','auth','child','conversation','guardian','app']):
        info=FEATURE_INFO[f]
        cards.append(dict(id=f,x=40+(i%2)*480,y=140+(i//2)*255,w=440,h=225,title=f'{i+1:02d}  '+info[0],
                          body=info[2]+'\n'+info[3]+'\n'+info[4],color='3' if f in ('child','app') else '4' if f=='guardian' else '5',
                          hint='담당 역할 · 주요 코드 보기 ↗', links=['[[Features/'+f+'|담당 역할 · 주요 코드]]'],uri=note_uri(g,'Features/'+f+'.md')))
    diagram(g,'00-Overview','DodamDodam  |  한눈에 보는 구현 구조','HTTP → 공통 검사 → Controller → Service → DB / 외부 연결',cards,[],1000,980,
            '기능별 책임 지도 · 노트의 링크 / Canvas 카드로 탐색 · 카드 배치는 호출 순서가 아님')


def write_flow(g,flow):
    """한 시나리오를 여섯 단계로 요약하고 정확한 메서드와 실패 경로에 연결한다."""
    cards=[]
    positions=[(40,150),(520,150),(520,450),(40,450),(40,750),(520,750)]
    lines=['# '+flow['title'],'','[[00-Start|시작]] → [[Features/'+flow['feature']+'|기능 개요]] → 이 흐름 → 메서드 정의','',
           '**진입:** `'+flow['trigger']+'`', '', '**결과:** '+flow['result'],'',
           '> [!important] 먼저 알아둘 점','> '+flow['caution'],'',
           '![[Assets/'+flow['id']+'.svg]]','', '[[Flows/'+flow['id']+'.canvas|같은 흐름을 Canvas에서 열기]] · 아래 코드 링크로 실제 메서드 이동','',
           '번호와 화살표는 **주요 처리 순서**다. 조건에 따른 조기 반환·실패와 중간 helper 호출은 생략했다. 직접 Java 호출 관계는 연결된 메서드 노트에서 확인한다.','',
           '## 단계별 코드','']
    for i,((title,body,refs),(x,y)) in enumerate(zip(flow['steps'],positions),1):
        mids=[resolve(g,ref) for ref in refs]
        links=[g.wiki(mid,ref) for mid,ref in zip(mids,refs)]
        cards.append(dict(id='step-'+str(i),x=x,y=y,w=440,h=255,title=f'{i:02d}  '+title,body=body,color='5',
                          hint='대표 메서드 ↗',links=[g.wiki(mid,g.METHODS[mid]['name']+'()') for mid in mids],uri=g.obsidian_uri(g.METHODS[mids[0]]['owner'],mids[0])))
        lines+=['### '+str(i)+'. '+title,'',body.replace('\n',' · '),'']+['- '+link for link in links]+['']
    edges=[('step-1','step-2','right','left'),('step-2','step-3','bottom','top'),('step-3','step-4','left','right'),
           ('step-4','step-5','bottom','top'),('step-5','step-6','right','left')]
    diagram(g,'Flows/'+flow['id'],flow['title'],flow['trigger'],cards,edges,1000,1120,
            '번호 순서로 읽기 · 주요 처리 순서 요약 · 직접 호출·분기 전체는 연결된 메서드 노트에서 확인')
    lines+=['## 실패하거나 달라지는 경우','','| 조건 | 결과 / 주의 | 확인할 코드 |','| --- | --- | --- |']
    for condition,result,ref in flow['failures']:
        lines+=['| '+condition+' | '+result+' | '+g.wiki(resolve(g,ref),ref)+' |']
    lines+=['','[[00-Start|다른 흐름 선택]] · [[Features/'+flow['feature']+'|관련 클래스 찾기]]']
    g.save('Flows/'+flow['id']+'.md','\n'.join(lines))


def write_feature_notes(g):
    """패키지 이름 대신 담당 책임·수정 지점·주요 진입점부터 보여준다."""
    for f,info in FEATURE_INFO.items():
        title,summary,_,_,state,flows,refs=info
        owners=[o for o in g.CLASSES if g.feature(o)==f]
        lines=['# '+title+' · '+f,'','[[00-Start|시작]] → 기능 개요 → 요청 흐름 → 메서드 정의','',summary,'',
               '> [!info] 현재 범위','> '+state,'','## 먼저 볼 흐름','']
        for fid in flows:
            flow=next(v for v in FLOWS if v['id']==fid)
            lines+=['- [[Flows/'+fid+'|'+flow['title']+']] — '+flow['result']]
        lines+=['','## 주요 진입점','','| 코드 | 책임 |','| --- | --- |']
        for ref in refs:
            mid=resolve(g,ref);m=g.METHODS[mid];c=g.CLASSES[m['owner']]
            lines+=['| '+g.wiki(mid,ref)+' | '+class_summary(c,[m])+' |']
        if f=='app':
            lines+=['','## 현재 연결과 후속 연결','','| 영역 | 현재 상태 |','| --- | --- |',
                    '| 실행 | Java 25 · Spring Boot · Servlet / Spring Security |',
                    '| DB | PostgreSQL · JdbcTemplate · Flyway V1~V7 · 현재 11개 테이블 |',
                    '| 메일 | SMTP 구현. 실제 공급자·환경 설정은 별도 확인 |',
                    '| AI | 이 backend에 STT / TTS / FastAPI 연결 구현 없음. AI 팀 저장소는 분석 범위 밖 |',
                    '| Supabase | 도입 검토 단계. 현재 연결로 그리지 않음 |','',
                    '[DB 테이블·현재 구성](../../architecture.md#8-실제-db와-후속-설계의-차이) · [Supabase 검토](../../supabase-assessment.md)']
        lines+=['','## 수정할 때 찾을 위치','','| 계층 | 담당 책임 | 클래스 |','| --- | --- | --- |']
        groups={}
        for owner in owners:
            c=g.CLASSES[owner]
            # 중첩·익명 타입은 접힌 전체 목록에 둔다.
            if '$' in owner or owner.removeprefix('com.dodamdodam.backend.').count('.')>2:continue
            rest=owner.removeprefix('com.dodamdodam.backend.').split('.')
            layer=rest[1] if len(rest)>2 else 'bootstrap'
            groups.setdefault(layer,[]).append(owner)
        descriptions={'api':'HTTP 입력·응답·기능 오류','application':'업무 규칙·실행 순서·트랜잭션','infrastructure':'DB·잠금·외부 시스템',
                      'dto':'요청 검증·전달 데이터','exception':'공개 업무 오류','session':'현재 인증 문맥·세션 수명','config':'프레임워크 구성',
                      'web':'서블릿 요청 필터','auth':'CSRF·보안 응답','error':'공통 오류 형식','validation':'공통 입력 검증','bootstrap':'애플리케이션 시작'}
        for layer,group in groups.items():
            entry_owners={g.METHODS[resolve(g,ref)]['owner'] for ref in refs}
            group.sort(key=lambda o:(o not in entry_owners,g.CLASSES[o]['name'] not in ROLES,o))
            links=['[['+g.class_path(o)[:-3]+'|'+g.CLASSES[o]['name']+']]' for o in group[:3]]
            remainder=' · 외 '+str(len(group)-3)+'개는 아래 전체 목록' if len(group)>3 else ''
            lines+=['| `'+layer+'` | '+descriptions.get(layer,'보조 타입')+' | '+' · '.join(links)+remainder+' |']
        lines+=['','> [!example]- 전체 타입 '+str(len(owners))+'개 — 중첩 타입·데이터 포함','>']
        lines+=['> - [['+g.class_path(o)[:-3]+'|'+o.removeprefix('com.dodamdodam.backend.')+']]' for o in owners]
        lines+=['','[[Canvases/'+f+'.canvas|전체 호출선 보기 — 세부 조사용]] · [[90-Reference|전체 자료와 분석 한계]]']
        g.save('Features/'+f+'.md','\n'.join(lines))


def write_start(g):
    """전체 목록을 뒤로 보내고 처음 읽을 순서와 작업별 진입점을 제공한다."""
    lines=['# DodamDodam · 처음 보는 담당자를 위한 코드 지도','',
           '**먼저 이 한 장으로 책임을 파악하고 → 맡은 요청 흐름을 선택하고 → 필요한 메서드로 들어가세요.**','',
           '> [!info] 기준과 범위','> 2026-11-17 확인 · SCRUM-77 작업을 포함한 2026-11-15 소스 스냅샷. 자동 갱신 문서는 아닙니다.','',
           '![[Assets/00-Overview.svg]]','','[[00-Overview.canvas|한눈에 보는 구조를 Canvas에서 열기]]','',
           '그림은 요약 보기다. **아래 링크 또는 Canvas 카드의 링크**로 들어가면 해당 메서드까지 이동할 수 있다.','',
           '## 맡은 작업부터 찾기','','| 확인하려는 것 | 먼저 열 문서 |','| --- | --- |']
    for flow in FLOWS:
        lines+=['| '+flow['title']+' | [[Flows/'+flow['id']+'|6단계 흐름 · 실제 메서드 · 실패 조건]] |']
    lines+=['','## 꼭 구분할 구현 범위','','| 현재 있는 것 | 이어서 필요한 것 |','| --- | --- |',
            '| 이메일 발급·SMTP·발급 실패 보상 | 인증번호 검증·회원가입·OAuth 계정 연동 |',
            '| 아이 등록 API·검증·소유권 검사 | **실제 characterId 목록 확정·연결** — 현재 운영 목록은 비어 있음 |',
            '| 대화 시작·재전송·종료 상태 조정 | 음성 API·수동 종료 API·타이머·요약 worker |',
            '| 보호자 권한 공통 검증 | PIN·기록 등 이를 사용하는 HTTP API |',
            '| PostgreSQL·SMTP 연결 구현 | STT / TTS / FastAPI 연결, Supabase 도입 여부 |','',
            '## Obsidian에서 읽는 순서','',
            '1. `backend/docs`를 보관소로 연다. 이 노트는 **읽기 보기**에서 보면 그림과 링크가 함께 보인다.',
            '2. 위 표에서 맡은 요청을 선택한다. 그림은 번호 순서대로 읽고, Canvas에서는 카드 안의 메서드 링크를 연다.',
            '3. 메서드 노트에서 `호출하는 곳 / 호출되는 곳`을 따라간다. 상단 `기능 개요`로 돌아올 수 있다.',
            '4. 전체 호출 누락이나 영향 범위를 조사할 때만 [[90-Reference|전체 클래스·메서드 지도]]를 연다.','',
            '## 기능별 담당 코드','']
    lines+=['- [[Features/'+f+'|'+info[0]+']] — '+info[1] for f,info in FEATURE_INFO.items()]
    lines+=['','[상세 아키텍처](../architecture.md) · [Supabase와 대안 비교](../supabase-assessment.md) · [[90-Reference|분석 범위·재생성·전체 목록]]']
    g.save('00-Start.md','\n'.join(lines))
    g.save('90-Reference.md','\n'.join([
        '# 전체 참조 자료와 분석 한계','','[[00-Start|처음 보는 담당자용 시작 화면]]','',
        '이 자료들은 **누락 없이 추적하기 위한 상세 지도**다. 전체를 한 화면에 맞추면 글자가 작아지므로 먼저 요청별 흐름을 읽고 필요한 구역을 확대한다.','',
        '- [[01-Class-Map.canvas|전체 클래스 Canvas]]', '- [[02-All-Methods.canvas|전체 메서드 Canvas]]',
        '- [[Assets/all-class-method-calls.svg|전체 클래스·메서드 SVG]]','',
        '타입 106개 · 메서드·생성자 566개 (작성 271 / 자동 295) · 내부 호출 쌍 541개 · 프레임워크 연결 42개.','',
        '## 표기와 한계','',
        '- 시작 그림: 기능 책임과 큰 흐름. 요청별 그림: 소스에서 확인한 주요 처리 순서로 중간 호출과 분기를 생략한다.',
        '- 전체 호출 지도: 실선은 호출·생성, 점선은 메서드 참조, 파선은 프레임워크 진입이다. 실행 시간순이 아니다.',
        '- 메서드 참조는 즉시 실행을 뜻하지 않는다. lambda 호출은 바깥 소스 메서드에 귀속한다.',
        '- reflection·가상 호출의 모든 구현을 확정하지 않는다. 호출자가 없다고 미사용 코드는 아니다.',
        '- 클래스 노트는 작성 메서드를 먼저 보여주고 생성자·자동 멤버 목록은 접는다. 정확한 정의 링크는 모두 유지한다.',
        '- 원본 코드는 backend/src/main/java에 있다. 노트의 코드 사본을 수정해도 제품 소스는 바뀌지 않는다.',
        '- SVG 클릭은 현재 컴퓨터의 Obsidian 경로를 사용한다. 다른 위치에 clone하면 재생성한다. Canvas·노트는 docs 보관소 상대 경로다.',
        '- 분석은 이 backend의 운영 Java 소스 대상이며 테스트와 의존성 라이브러리의 전체 정의는 포함하지 않는다.','',
        '## 재생성','',
        '코드가 변경되면 먼저 method-map.json을 다시 분석한다. 생성기는 소스 해시가 다르면 중단한다. 이후 저장소 루트에서 실행한다.','',
        '```sh','python3 docs/analysis/2026-11-15-codebase/support/generate_call_vault.py',
        'python3 docs/analysis/2026-11-15-codebase/support/verify_call_vault.py','```','',
        '역할과 요청 흐름 설명은 support/call_vault_guide.py에서 관리한다. 재생성 시 메서드 존재는 검사하지만, 코드 의미가 바뀌면 사람이 설명도 함께 검토해야 한다.',
        '커뮤니티 플러그인·외부 서비스 업로드 없이 기본 노트·Canvas로 사용한다.','',
        '[JSON Canvas 규격](https://jsoncanvas.org/spec/1.0/) · [Obsidian Canvas](https://obsidian.md/help/plugins/canvas)'
    ]))
