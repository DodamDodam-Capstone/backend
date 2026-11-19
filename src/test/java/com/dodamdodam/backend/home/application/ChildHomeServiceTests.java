package com.dodamdodam.backend.home.application;

import com.dodamdodam.backend.auth.session.AuthenticatedSession;
import com.dodamdodam.backend.auth.session.SessionSecurityRepository;
import com.dodamdodam.backend.child.application.ChildService;
import com.dodamdodam.backend.child.dto.ChildContext;
import com.dodamdodam.backend.child.exception.ChildException;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator;
import com.dodamdodam.backend.conversation.application.ConversationClosureCoordinator.Trigger;
import com.dodamdodam.backend.conversation.exception.ConversationException;
import com.dodamdodam.backend.conversation.infrastructure.ConversationClosureRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository;
import com.dodamdodam.backend.conversation.infrastructure.ConversationRepository.StoredConversation;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** DB·HTTP 없이 기존 협력 객체의 상태와 잠금 대기 후 시각을 조합한다. */
class ChildHomeServiceTests {

    private static final AuthenticatedSession SESSION = new AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID());
    private static final UUID CHILD = UUID.randomUUID();
    private static final UUID CONVERSATION = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-03T13:00:00+09:00");
    private static final OffsetDateTime MIDNIGHT = OffsetDateTime.parse("2026-10-04T00:00:00+09:00");
    private static final ChildContext CONTEXT = new ChildContext(CHILD, "가나", "별이", LocalDate.of(2020, 5, 12),
            List.of("공룡"), "test-character", OffsetDateTime.parse("2026-10-01T06:00:00+09:00"));
    private final SessionSecurityRepository security = mock(SessionSecurityRepository.class);
    private final ChildService children = mock(ChildService.class);
    private final ConversationRepository conversations = mock(ConversationRepository.class);
    private final ConversationClosureRepository expiration = mock(ConversationClosureRepository.class);
    private final ConversationClosureCoordinator closure = mock(ConversationClosureCoordinator.class);
    private final ChildHomeService service = new ChildHomeService(security, children, conversations, expiration, closure);

    @BeforeEach
    void prepare() {
        when(children.getChildContext(CHILD)).thenReturn(CONTEXT);
        when(conversations.currentTime()).thenReturn(NOW);
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.empty());
    }

    @AfterEach
    void neverCreatesRowsOrDirectlyChangesClosureAndRecovery() {
        verify(children, never()).create(any(), any());
        verify(conversations, never()).insertActive(any(), any(), any(), any(), any());
        verify(conversations, never()).insertLink(any(), any(), any());
        verify(expiration, never()).beginClosing(any(), any(), any());
        verify(expiration, never()).finishClosing(any(), any(), any());
        verify(expiration, never()).grantRecovery(any(), any(), any(), any());
    }

    @Test
    void mapsTheOwnedChildThroughTheExistingDtoFactory() {
        var home = service.getHome(SESSION, CHILD);

        assertThat(home.child().childId()).isEqualTo(CHILD);
        assertThat(home.child().name()).isEqualTo("가나");
        assertThat(home.child().nickname()).isEqualTo("별이");
        assertThat(home.child().characterId()).isEqualTo("test-character");
        assertThat(home.greeting()).isEqualTo("별이, 오늘도 반가워!");
        assertThat(home.joinedAt()).isEqualTo(CONTEXT.createdAt());
        assertThat(home.daysTogether()).isEqualTo(3);
        assertThat(home.availability().serverTime()).isEqualTo(NOW);
        assertThat(home.availability().canEnter()).isTrue();
        assertThat(home.activeConversationId()).isNull();
        verifyNoInteractions(expiration, closure);
    }

    @Test
    void checksContextAndOwnershipBeforeProfileAndConversationAndLocksTurnsBeforeLinks() {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(active()));
        when(conversations.hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION)).thenReturn(true);

        service.getHome(SESSION, CHILD);

        var order = inOrder(security, children, conversations, expiration);
        order.verify(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());
        order.verify(children).requireChildAccessForUpdate(SESSION.accountId(), CHILD);
        order.verify(children).getChildContext(CHILD);
        order.verify(conversations).findUnendedForUpdate(CHILD);
        order.verify(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());
        order.verify(conversations).currentTime();
        order.verify(expiration).lockProcessingTurns(CONVERSATION);
        order.verify(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());
        order.verify(conversations).currentTime();
        order.verify(expiration).expireProcessing(CONVERSATION, NOW);
        order.verify(conversations).hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION);
        order.verify(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());
    }

    @Test
    void invalidLoginCannotReadOrCleanAnotherResource() {
        var denied = new InsufficientAuthenticationException("로그인이 필요합니다.");
        doThrow(denied).when(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());

        assertThatThrownBy(() -> service.getHome(SESSION, CHILD)).isSameAs(denied);

        verifyNoInteractions(children, conversations, expiration, closure);
    }

    @Test
    void missingOrUnownedChildStopsBeforeContextAndConversationReads() {
        var denied = ChildException.notFound();
        doThrow(denied).when(children).requireChildAccessForUpdate(SESSION.accountId(), CHILD);

        assertThatThrownBy(() -> service.getHome(SESSION, CHILD)).isSameAs(denied);

        verify(children, never()).getChildContext(any());
        verifyNoInteractions(conversations, expiration, closure);
    }

    @Test
    void rechecksLoginAfterWaitingForChildAndConversationLocksBeforeCleanup() {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(closing()));
        doNothing().doThrow(new InsufficientAuthenticationException("로그인이 필요합니다."))
                .when(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());

        assertThatThrownBy(() -> service.getHome(SESSION, CHILD)).isInstanceOf(InsufficientAuthenticationException.class);

        verifyNoInteractions(expiration, closure);
        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exposesTodaysActiveIdOnlyForTheCurrentContextLink(boolean linked) {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(active()));
        when(conversations.hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION)).thenReturn(linked);

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.activeConversationId()).isEqualTo(linked ? CONVERSATION : null);
        assertThat(home.availability().canEnter()).isTrue();
        assertThat(home.availability().reason()).isNull();
        verify(conversations).hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION);
        verify(expiration).lockProcessingTurns(CONVERSATION);
        verify(expiration).expireProcessing(CONVERSATION, NOW);
        verifyNoMoreInteractions(expiration);
        verifyNoInteractions(closure);
    }

    @Test
    void usesTheKstServiceDateWhenTheDatabaseClockHasAUtcOffset() {
        when(conversations.currentTime()).thenReturn(OffsetDateTime.parse("2026-10-02T23:00:00Z"));
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(active()));
        when(conversations.hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION)).thenReturn(true);

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.activeConversationId()).isEqualTo(CONVERSATION);
        assertThat(home.availability().serverTime()).isEqualTo(OffsetDateTime.parse("2026-10-03T08:00:00+09:00"));
    }

    @Test
    void doesNotExposeAnActiveIdFromADifferentServiceDate() {
        var future = new StoredConversation(CONVERSATION, "ACTIVE", NOW.plusDays(1),
                NOW.toLocalDate().plusDays(1), MIDNIGHT.plusDays(1));
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(future));

        assertThat(service.getHome(SESSION, CHILD).activeConversationId()).isNull();

        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @Test
    void keepsClosingWhenTheCoordinatorStillFindsPendingWorkWithoutExpiringTwice() {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(closing()));

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().canEnter()).isFalse();
        assertThat(home.availability().reason()).isEqualTo("CONVERSATION_CLOSING");
        assertThat(home.activeConversationId()).isNull();
        verify(closure).reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE);
        verifyNoMoreInteractions(closure);
        verifyNoInteractions(expiration);
        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @Test
    void reloadsAfterTheCoordinatorFinishesClosingInsteadOfInferringCompletion() {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(closing())).thenReturn(Optional.empty());

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().canEnter()).isTrue();
        assertThat(home.availability().reason()).isNull();
        assertThat(home.activeConversationId()).isNull();
        verify(closure).reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE);
        verifyNoInteractions(expiration);
    }

    @Test
    void endedRecordsExcludedByTheUnendedQueryDoNotBlockEntry() {
        // 기존 SQL은 ACTIVE/CLOSING만 반환하므로 ENDED만 남은 경우 조회 결과는 비어 있다.
        assertThat(service.getHome(SESSION, CHILD).availability().canEnter()).isTrue();

        verifyNoInteractions(expiration, closure);
        verify(conversations, never()).findRequestForUpdate(any(), any());
        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"00:00:00", "07:59:59"})
    void outsideHoursTakePriorityEvenWhenClosingRemains(String time) {
        when(conversations.currentTime()).thenReturn(OffsetDateTime.parse("2026-10-03T" + time + "+09:00"));
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(closing()));

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().reason()).isEqualTo("OUTSIDE_SERVICE_HOURS");
        assertThat(home.availability().canEnter()).isFalse();
        assertThat(home.availability().nextOpensAt()).isEqualTo(OffsetDateTime.parse("2026-10-03T08:00:00+09:00"));
        assertThat(home.activeConversationId()).isNull();
        verify(closure).reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE);
        verifyNoInteractions(expiration);
        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void delegatesPreviousDatesActiveToTheCoordinatorAndUsesTheReloadedState(boolean stillClosing) {
        var previous = new StoredConversation(CONVERSATION, "ACTIVE", NOW.minusDays(1),
                NOW.toLocalDate().minusDays(1), MIDNIGHT.minusDays(1));
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(previous))
                .thenReturn(stillClosing ? Optional.of(closing()) : Optional.empty());

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().canEnter()).isEqualTo(!stillClosing);
        assertThat(home.availability().reason()).isEqualTo(stillClosing ? "CONVERSATION_CLOSING" : null);
        assertThat(home.activeConversationId()).isNull();
        verify(closure).reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE);
        verifyNoMoreInteractions(closure);
        verifyNoInteractions(expiration);
    }

    @Test
    void crossingMidnightWhileWaitingForTurnLocksUsesOnlyTheCoordinatorExpiration() {
        var before = MIDNIGHT.minusNanos(1_000);
        when(conversations.currentTime()).thenReturn(before, MIDNIGHT);
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(active())).thenReturn(Optional.of(closing()));

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().reason()).isEqualTo("OUTSIDE_SERVICE_HOURS");
        assertThat(home.activeConversationId()).isNull();
        verify(expiration).lockProcessingTurns(CONVERSATION);
        verify(expiration, never()).expireProcessing(any(), any());
        verify(closure).reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE);
        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @Test
    void crossingMidnightWhileWaitingForTheLinkDiscardsTheIdAndObservesTheBoundary() {
        var before = MIDNIGHT.minusNanos(1_000);
        when(conversations.currentTime()).thenReturn(before, before, before, MIDNIGHT);
        when(conversations.findUnendedForUpdate(CHILD))
                .thenReturn(Optional.of(active())).thenReturn(Optional.of(active())).thenReturn(Optional.of(closing()));
        when(conversations.hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION)).thenReturn(true);

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().serverTime()).isEqualTo(MIDNIGHT);
        assertThat(home.availability().reason()).isEqualTo("OUTSIDE_SERVICE_HOURS");
        assertThat(home.activeConversationId()).isNull();
        var order = inOrder(expiration, conversations, closure);
        order.verify(expiration).lockProcessingTurns(CONVERSATION);
        order.verify(expiration).expireProcessing(CONVERSATION, before);
        order.verify(conversations).hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION);
        order.verify(closure).reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE);
        verifyNoMoreInteractions(closure);
    }

    @Test
    void doesNotPublishACandidateThatNoLongerMatchesTheFinalServiceDate() {
        when(conversations.currentTime()).thenReturn(NOW, NOW, NOW, NOW.minusDays(1));
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(active()));
        when(conversations.hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION)).thenReturn(true);

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().serviceDate()).isEqualTo(LocalDate.of(2026, 10, 2));
        assertThat(home.activeConversationId()).isNull();
    }

    @Test
    void finalLoginCheckRejectsAContextThatExpiresWhileWaitingForTheLink() {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(active()));
        var invalid = new AtomicBoolean();
        doAnswer(_ -> {
            if (invalid.get()) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
            return null;
        }).when(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());
        when(conversations.hasLinkForUpdate(SESSION.securityContextId(), CONVERSATION)).thenAnswer(_ -> {
            invalid.set(true);
            return true;
        });

        assertThatThrownBy(() -> service.getHome(SESSION, CHILD)).isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void usesTheFinalDatabaseTimeWhenTheClockCrossesOpeningDuringTheRequest() {
        when(conversations.currentTime()).thenReturn(OffsetDateTime.parse("2026-10-03T07:59:59+09:00"),
                OffsetDateTime.parse("2026-10-03T08:00:00+09:00"));

        var home = service.getHome(SESSION, CHILD);

        assertThat(home.availability().canEnter()).isTrue();
        assertThat(home.availability().serverTime()).isEqualTo(OffsetDateTime.parse("2026-10-03T08:00:00+09:00"));
    }

    @Test
    void propagatesCoordinatorStorageFailureWithoutReadingALinkOrMakingADto() {
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(closing()));
        var failure = new ConversationException(new IllegalStateException("합성 저장 실패"));
        when(closure.reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE)).thenThrow(failure);

        assertThatThrownBy(() -> service.getHome(SESSION, CHILD)).isSameAs(failure);

        verifyNoInteractions(expiration);
        verify(conversations, never()).hasLinkForUpdate(any(), any());
    }

    @Test
    void springProxyStartsAWritableTransactionAndCommitsSuccessfulObservation() {
        var transactions = mock(PlatformTransactionManager.class);
        var status = mock(TransactionStatus.class);
        when(transactions.getTransaction(any())).thenReturn(status);

        transactional(transactions).getHome(SESSION, CHILD);

        var definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactions).getTransaction(definition.capture());
        assertThat(definition.getValue().isReadOnly()).isFalse();
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
        verify(transactions).commit(status);
        verify(transactions, never()).rollback(any());
    }

    @Test
    void springProxyRollsBackCleanupWhenTheFinalContextCheckFails() {
        var transactions = mock(PlatformTransactionManager.class);
        var status = mock(TransactionStatus.class);
        when(transactions.getTransaction(any())).thenReturn(status);
        when(conversations.findUnendedForUpdate(CHILD)).thenReturn(Optional.of(closing()));
        var invalid = new AtomicBoolean();
        doAnswer(_ -> {
            if (invalid.get()) {
                throw new InsufficientAuthenticationException("로그인이 필요합니다.");
            }
            return null;
        }).when(security).requireCurrentContextForUpdate(SESSION.accountId(), SESSION.securityContextId());
        when(closure.reconcile(SESSION.accountId(), CHILD, CONVERSATION, Trigger.OBSERVE)).thenAnswer(_ -> {
            invalid.set(true);
            return null;
        });

        assertThatThrownBy(() -> transactional(transactions).getHome(SESSION, CHILD))
                .isInstanceOf(InsufficientAuthenticationException.class);

        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
    }

    /** 실제 DB 없이 Spring의 애노테이션 기반 Tx 적용·예외 전파만 검증한다. */
    private ChildHomeService transactional(PlatformTransactionManager transactions) {
        var interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(interceptor);
        return (ChildHomeService) proxy.getProxy();
    }

    /** 시작 키·발화·연결을 새로 만들지 않는 조회 상태만 제공한다. */
    private static StoredConversation active() {
        return new StoredConversation(CONVERSATION, "ACTIVE", NOW.withHour(8), NOW.toLocalDate(), MIDNIGHT);
    }

    private static StoredConversation closing() {
        return new StoredConversation(CONVERSATION, "CLOSING", NOW.withHour(8), NOW.toLocalDate(), MIDNIGHT);
    }
}
