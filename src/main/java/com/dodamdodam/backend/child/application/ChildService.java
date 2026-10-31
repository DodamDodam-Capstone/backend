package com.dodamdodam.backend.child.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.dto.ChildContext;
import com.dodamdodam.backend.child.dto.ChildView;
import com.dodamdodam.backend.child.dto.CreateChildRequest;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.child.infrastructure.ChildRepository;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChildService {

    // shortcut: 실제 캐릭터 목록 미정으로 등록을 거부함, 목록 확정 시 승인된 ID를 연결한다.
    private static final Set<String> SUPPORTED_CHARACTER_IDS = Set.of();

    private final ChildRepository repository;
    private final SessionSecurityRepository securityRepository;
    private final Clock clock;
    private final Set<String> supportedCharacterIds;

    /** 운영 시계와 현재 지원 목록으로 서비스를 구성한다. */
    @Autowired
    public ChildService(ChildRepository repository, SessionSecurityRepository securityRepository) {
        this(repository, securityRepository, Clock.systemUTC(), SUPPORTED_CHARACTER_IDS);
    }

    /** 날짜 경계와 지원 목록을 테스트에서 고정할 수 있도록 의존성을 주입한다. */
    ChildService(ChildRepository repository, SessionSecurityRepository securityRepository,
                 Clock clock, Set<String> supportedCharacterIds) {
        this.repository = repository;
        this.securityRepository = securityRepository;
        this.clock = clock;
        this.supportedCharacterIds = Set.copyOf(supportedCharacterIds);
    }

    /** 입력 검증·현재 문맥 확인·등록을 같은 트랜잭션에서 수행한다. */
    @Transactional
    public ChildView create(AuthenticatedSession session, CreateChildRequest request) {
        var normalized = request.normalized(clock, supportedCharacterIds);
        // 입력 검증 후 문맥을 잠가 폐기·계정 버전 변경과 등록이 엇갈리지 않게 한다.
        securityRepository.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        // 계정당 UNIQUE 충돌만 등록 제한이며 다른 저장 장애는 원래 업무 예외를 유지한다.
        return repository.insert(session.accountId(), normalized).orElseThrow(ChildException::limitReached);
    }

    /** 소유 자녀를 반환하며, 미소유와 미존재를 같은 오류로 처리한다. */
    public ChildView requireChildAccess(UUID accountId, UUID childId) {
        return repository.findOwned(accountId, childId).orElseThrow(ChildException::notFound);
    }

    /** requireAuthenticatedSession → requireChildAccess 순서로 검증한 뒤 호출하는 내부 조회다. */
    public ChildContext getChildContext(UUID childId) {
        var child = repository.findById(childId).orElseThrow(ChildException::notFound);
        return new ChildContext(child.childId(), child.name(), child.nickname(), child.birthDate(),
                child.interests(), child.characterId(), child.createdAt());
    }
}
