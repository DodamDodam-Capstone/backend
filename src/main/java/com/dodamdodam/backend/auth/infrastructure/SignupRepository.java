package com.dodamdodam.backend.auth.infrastructure;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SignupRepository {
    private final JdbcTemplate jdbc;

    /** 가입 트랜잭션에서 현재 JDBC 연결을 사용한다. */
    public SignupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 기존 계정을 보존하며 이메일 유일 제약으로 동시 생성의 승자를 결정한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean insert(UUID id, String verifiedEmail, String passwordHash) {
        return jdbc.update("""
                INSERT INTO accounts(id, email, password_hash) VALUES (?, ?, ?)
                ON CONFLICT (email) DO NOTHING
                """, id, verifiedEmail, passwordHash) == 1;
    }
}
