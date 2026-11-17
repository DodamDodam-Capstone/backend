package com.dodamdodam.backend.auth.application;

import com.dodamdodam.backend.auth.exception.EmailVerificationException;
import com.dodamdodam.backend.auth.infrastructure.EmailVerificationRepository;
import com.dodamdodam.backend.auth.infrastructure.SmtpEmailVerificationSender;
import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@Service
public class EmailVerificationDeliveryService {
    private final SmtpEmailVerificationSender sender;
    private final EmailVerificationRepository repository;
    private final TransactionTemplate compensation;

    /** 발송 실패 시 이미 커밋한 그 발급만 폐기하는 별도 Tx를 구성한다. */
    public EmailVerificationDeliveryService(SmtpEmailVerificationSender sender, EmailVerificationRepository repository,
                                             PlatformTransactionManager manager) {
        this.sender = sender;
        this.repository = repository;
        this.compensation = new TransactionTemplate(manager);
        this.compensation.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 업무 Tx 종료 뒤 발송하고 거부·timeout·결과 불명확은 해당 id만 보상 폐기한다. */
    public void deliver(UUID challengeId, String email, String code, boolean eligible) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new AuthStateUnavailableException();
        }
        try {
            sender.send(email, code, eligible);
        }
        catch (RuntimeException failure) {
            // SMTP 예외는 이메일·번호를 포함할 수 있어 원인으로도 보관하지 않는다.
            invalidate(challengeId);
            throw EmailVerificationException.deliveryUnavailable();
        }
    }

    /** 늦은 발송 실패나 권한 변경에서도 새 발급·소비 완료 증명을 건드리지 않는다. */
    public void invalidate(UUID challengeId) {
        try {
            compensation.executeWithoutResult(status -> repository.invalidateUnconsumed(challengeId));
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            throw new AuthStateUnavailableException(failure);
        }
    }
}
