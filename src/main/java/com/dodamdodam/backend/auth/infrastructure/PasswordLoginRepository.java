package com.dodamdodam.backend.auth.infrastructure;

import com.dodamdodam.backend.auth.session.AuthStateUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.convert.ConversionException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionException;

@Repository
public class PasswordLoginRepository {
    private final JdbcTemplate jdbc;

    /** 기존 JDBC 연결로 비밀번호 확인에 필요한 최소 자격만 조회한다. */
    public PasswordLoginRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 정규화된 이메일의 자격을 잠금 없이 읽고 실제 세션 생성 시 다시 검사한다. */
    public Optional<Credential> findCredential(String normalizedEmail) {
        try {
            return jdbc.query("""
                    SELECT id, password_hash, session_version FROM accounts WHERE email = ?
                    """, (result, row) -> new Credential(result.getObject("id", UUID.class),
                    result.getString("password_hash"), result.getLong("session_version")), normalizedEmail)
                    .stream().findFirst();
        }
        catch (DataAccessException | TransactionException | ConversionException failure) {
            // 조회 실패를 계정 부존재로 바꾸지 않는다.
            throw new AuthStateUnavailableException(failure);
        }
    }

    /** 해시 비교 이후 계정 잠금 안에서 재확인할 자격 스냅샷이다. */
    public record Credential(UUID accountId, String passwordHash, long sessionVersion) {
        /** 진단 로그에서 계정 식별자와 저장 해시를 숨긴다. */
        @Override
        public String toString() {
            return "Credential[redacted]";
        }
    }
}
