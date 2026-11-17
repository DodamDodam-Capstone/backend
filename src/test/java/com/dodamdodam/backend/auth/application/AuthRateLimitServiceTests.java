package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.domain.AuthRateLimitKey.Action;
import com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRateLimitServiceTests {

    @ParameterizedTest
    @ValueSource(strings = {"BEGIN", "COMMIT"})
    void transactionBoundaryFailureIsUnavailableRatherThanSuccessfulReservation(String phase) {
        // 외부 Tx 관리자의 시작/커밋 장애만 주입하며 예약 규칙은 PostgreSQL 테스트에서 확인한다.
        var manager = mock(PlatformTransactionManager.class);
        var repository = mock(AuthRateLimitRepository.class);
        if (phase.equals("BEGIN")) {
            when(manager.getTransaction(any(TransactionDefinition.class)))
                    .thenThrow(new CannotCreateTransactionException("synthetic transaction detail"));
        }
        else {
            var status = mock(TransactionStatus.class);
            when(manager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
            doThrow(new TransactionSystemException("synthetic commit detail")).when(manager).commit(status);
        }
        var service = new AuthRateLimitService(repository, manager);

        assertThatThrownBy(() -> service.reserve(List.of(new AuthRateLimitKey(Action.API_IP, new byte[32]))))
                .isInstanceOfSatisfying(AuthStateUnavailableException.class, failure -> {
                    assertThat(failure.getMessage()).doesNotContain("synthetic");
                    assertThat(failure.getCause()).isInstanceOf(org.springframework.transaction.TransactionException.class);
                });
    }
}
