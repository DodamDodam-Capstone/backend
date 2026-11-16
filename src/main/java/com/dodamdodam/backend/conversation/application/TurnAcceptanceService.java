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
import com.dodamdodam.backend.conversation.infrastructure.TurnProcessingProperties;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;
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
        return Objects.requireNonNull(transaction.execute(_ -> acceptInTransaction(session, childId, conversationId, clientRequestId, hash)));
    }

    /** 상위 행부터 잠그고 접근 검사를 통과한 뒤에만 키·해시·PROCESSING 상태를 판정한다. */
    private Acceptance acceptInTransaction(AuthenticatedSession session, UUID childId, UUID conversationId, UUID clientRequestId, String hash) {
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
            return new Acceptance(stored.view(), stored.processingDeadlineAt(), false);
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
        return new Acceptance(stored.view(), stored.processingDeadlineAt(), true);
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

    /** HTTP 응답이 아닌 내부 결과다. 최초 커밋의 newlyAccepted만 후속 AI 1회 시도 자격이며 재시작 복구권이 아니다. */
    public record Acceptance(TurnView turn, OffsetDateTime processingDeadlineAt, boolean newlyAccepted) { }
}
