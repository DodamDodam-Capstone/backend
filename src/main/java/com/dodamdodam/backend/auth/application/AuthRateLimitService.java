package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.domain.AuthRateLimitKey;
import com.dodamdodam.backend.auth.exception.AuthRateLimitException;
import com.dodamdodam.backend.auth.infrastructure.AuthRateLimitRepository;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.List;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AuthRateLimitService {

    private final AuthRateLimitRepository repository;
    private final TransactionTemplate reservation;

    /** 이후 업무 rollback과 독립적으로 예산을 커밋하는 짧은 예약 Tx를 구성한다. */
    public AuthRateLimitService(AuthRateLimitRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.reservation = new TransactionTemplate(transactionManager);
        this.reservation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 업무 row lock 전에 호출하며 API_IP 단독 예약 뒤 같은 단계의 업무 키들을 함께 예약한다. */
    public void reserve(List<AuthRateLimitKey> keys) {
        Long retryAfter;
        try {
            // 메서드 본문뿐 아니라 Tx 시작·커밋 실패도 인증 상태 장애로 구분한다.
            retryAfter = reservation.execute(status -> repository.reserve(keys));
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
        if (retryAfter == null) {
            throw new AuthStateUnavailableException();
        }
        if (retryAfter > 0) {
            throw new AuthRateLimitException(retryAfter);
        }
    }
}
