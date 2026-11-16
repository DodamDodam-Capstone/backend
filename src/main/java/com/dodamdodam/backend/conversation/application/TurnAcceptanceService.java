package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.domain.ConversationServiceHours;
import com.dodamdodam.backend.conversation.dto.TurnView;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation;
import com.dodamdodam.backend.conversation.infrastructure.ConversationTurnRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationTurnRepository.StoredTurn;
import com.dodamdodam.backend.conversation.infrastructure.TurnProcessingProperties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@EnableConfigurationProperties(TurnProcessingProperties.class)
public class TurnAcceptanceService {

    private final SessionSecurityRepository security;
    private final ChildService children;
    private final ConversationRepository conversations;
    private final ConversationTurnRepository turns;
    private final TurnProcessingProperties properties;
    private final TransactionTemplate transaction;

    /** 공통 접근·DB 시각과 발화 저장소를 하나의 짧은 접수 트랜잭션에 연결한다. */
    public TurnAcceptanceService(SessionSecurityRepository security, ChildService children, ConversationRepository conversations,
            ConversationTurnRepository turns, TurnProcessingProperties properties, PlatformTransactionManager transactions) {
        this.security = security;
        this.children = children;
        this.conversations = conversations;
        this.turns = turns;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactions);
    }

    /** 검증된 서버 로그인 문맥과 원본 audio 바이트를 받으며 신규 실행 자격은 접수 커밋 이후에만 반환한다. */
    @Transactional(propagation = Propagation.NEVER)
    public Acceptance accept(AuthenticatedSession session, UUID childId, UUID conversationId, UUID clientRequestId, byte[] originalAudio) {
        Objects.requireNonNull(session, "서버 로그인 문맥이 필요합니다.");
        Objects.requireNonNull(childId, "아이 식별자가 필요합니다.");
        Objects.requireNonNull(conversationId, "대화 식별자가 필요합니다.");
        Objects.requireNonNull(clientRequestId, "발화 요청 키가 필요합니다.");
        // 파일 처리·해시는 업무 잠금 전에 끝내며 메타데이터나 재인코딩 결과를 입력에 섞지 않는다.
        var hash = audioHash(Objects.requireNonNull(originalAudio, "원본 음성 바이트가 필요합니다."));
        var decision = Objects.requireNonNull(transaction.execute(_ -> acceptInTransaction(session, childId, conversationId, clientRequestId, hash)));
        // 커밋 성공 후에만 최초 실행자에게 외부 입력으로 만들 수 없는 결과 저장 문맥을 발급한다.
        var permit = decision.created()
                ? Optional.of(new AcceptedTurn(session.accountId(), session.securityContextId(), childId, conversationId,
                        decision.stored().view().turnId()))
                : Optional.<AcceptedTurn>empty();
        return new Acceptance(decision.stored().view(), decision.stored().processingDeadlineAt(), permit);
    }

    /** 상위 행부터 잠그고 접근 검사를 통과한 뒤에만 키·해시·PROCESSING 상태를 판정한다. */
    private AcceptanceDecision acceptInTransaction(AuthenticatedSession session, UUID childId, UUID conversationId,
                                                   UUID clientRequestId, String hash) {
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        children.requireChildAccessForUpdate(session.accountId(), childId);
        var conversation = conversations.findForUpdate(childId, conversationId).orElseThrow(ChildException::notFound);
        // 하위 잠금 대기 중 만료된 로그인은 시간·상태 오류보다 먼저 거부한다.
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        requireAccepting(conversation, conversations.currentTime());

        // 발화→연결 순서로 잠그되, 아직 기존 결과를 반환하거나 해시 충돌을 공개하지 않는다.
        var existing = turns.findRequestForUpdate(conversationId, clientRequestId);
        boolean processing = existing.isEmpty() && turns.hasProcessingForUpdate(conversationId);
        boolean linked = conversations.hasLinkForUpdate(session.securityContextId(), conversationId);
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        var checkedAt = conversations.currentTime();
        requireAccepting(conversation, checkedAt);
        if (!linked) {
            throw ConversationException.childSessionRequired();
        }
        if (existing.isPresent()) {
            var stored = existing.orElseThrow();
            if (!stored.requestHash().equals(hash)) {
                throw ConversationException.idempotencyConflict();
            }
            return new AcceptanceDecision(stored, false);
        }
        if (processing) {
            throw ConversationException.turnInProgress();
        }

        int sequence = turns.nextSequence(conversationId);
        // 순번 조회 이후 DB 시각으로 최초 기한을 확정하고 재전송에서는 다시 계산하지 않는다.
        var createdAt = conversations.currentTime();
        requireAccepting(conversation, createdAt);
        var stored = turns.insertProcessing(UUID.randomUUID(), conversationId, clientRequestId, hash, sequence,
                createdAt, createdAt.plus(properties.processingTimeout()));
        security.requireCurrentContextForUpdate(session.accountId(), session.securityContextId());
        requireAccepting(conversation, conversations.currentTime());
        return new AcceptanceDecision(stored, true);
    }

    /** 아이 내용 경로의 ENDED→이용시간→CLOSING/날짜 경계 우선순위를 유지한다. */
    private void requireAccepting(StoredConversation conversation, OffsetDateTime checkedAt) {
        if ("ENDED".equals(conversation.status())) {
            throw ConversationException.ended();
        }
        var window = ConversationServiceHours.requireOpen(checkedAt);
        if (!"ACTIVE".equals(conversation.status()) || !window.serviceDate().equals(conversation.serviceDate())
                || !checkedAt.isBefore(conversation.scheduledEndAt())) {
            throw ConversationException.closing();
        }
    }

    /** 수신한 audio 파트의 실제 바이트만 표준 SHA-256 소문자 hex64로 변환한다. */
    private String audioHash(byte[] originalAudio) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(originalAudio));
        }
        catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("표준 SHA-256을 사용할 수 없습니다.", failure);
        }
    }

    /** HTTP 응답이 아닌 내부 결과이며 재전송에는 결과 저장 문맥을 새로 발급하지 않는다. */
    public static final class Acceptance {
        private final TurnView turn;
        private final OffsetDateTime processingDeadlineAt;
        private final Optional<AcceptedTurn> completionPermit;

        /** 접수 커밋 이후 서비스 내부에서만 생성한다. */
        private Acceptance(TurnView turn, OffsetDateTime processingDeadlineAt, Optional<AcceptedTurn> completionPermit) {
            this.turn = turn;
            this.processingDeadlineAt = processingDeadlineAt;
            this.completionPermit = completionPermit;
        }

        /** 접근 검사를 통과한 접수 시점의 공개 발화 정보다. */
        public TurnView turn() { return turn; }

        /** 재전송으로 갱신하지 않는 DB의 원래 기한이다. */
        public OffsetDateTime processingDeadlineAt() { return processingDeadlineAt; }

        /** 최초 커밋 실행자의 1회 AI 시도 자격이며 재시작 복구권이 아니다. */
        public boolean newlyAccepted() { return completionPermit.isPresent(); }

        /** 최초 실행자의 후속 결과 저장에만 전달하며 HTTP 입력·응답으로 사용하지 않는다. */
        public Optional<AcceptedTurn> completionPermit() { return completionPermit; }
    }

    /** 검증된 최초 접수에 묶인 내부 문맥이다. 외부 호출자는 ID를 조합해 생성할 수 없다. */
    public static final class AcceptedTurn {
        private final UUID accountId;
        private final UUID securityContextId;
        private final UUID childId;
        private final UUID conversationId;
        private final UUID turnId;

        /** 최초 접수 서비스만 생성하며 원본 음성·해시·텍스트를 보관하지 않는다. */
        private AcceptedTurn(UUID accountId, UUID securityContextId, UUID childId, UUID conversationId, UUID turnId) {
            this.accountId = accountId;
            this.securityContextId = securityContextId;
            this.childId = childId;
            this.conversationId = conversationId;
            this.turnId = turnId;
        }

        /** 결과 저장의 최상위 잠금 대상이다. */
        UUID accountId() { return accountId; }

        /** 현재 로그인 검증과 구분하는 최초 접수 문맥이다. */
        UUID securityContextId() { return securityContextId; }

        /** 최초 접수에서 소유권을 확인한 아이다. */
        UUID childId() { return childId; }

        /** 결과를 다른 대화에 섞지 않기 위한 원래 대상이다. */
        UUID conversationId() { return conversationId; }

        /** 최초 커밋한 PROCESSING 발화만 식별한다. */
        UUID turnId() { return turnId; }

        /** 내부 ID나 내용을 진단 문자열로 노출하지 않는다. */
        @Override
        public String toString() { return "AcceptedTurn[internal]"; }
    }

    /** 커밋 전 판정은 외부로 반환하거나 실행 자격으로 사용하지 않는다. */
    private record AcceptanceDecision(StoredTurn stored, boolean created) { }
}
