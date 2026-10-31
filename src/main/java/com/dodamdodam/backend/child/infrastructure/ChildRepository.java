package com.dodamdodam.backend.child.infrastructure;

import com.dodamdodam.backend.child.dto.ChildView;
import com.dodamdodam.backend.child.dto.CreateChildRequest;
import com.dodamdodam.backend.child.exception.ChildException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.SqlArrayValue;
import org.springframework.stereotype.Repository;

@Repository
public class ChildRepository {

    private static final String COLUMNS = "id, name, nickname, birth_date, gender, interests, character_id, created_at";
    private static final RowMapper<ChildView> CHILD_ROW = (result, row) -> {
        var interests = result.getArray("interests");
        try {
            return new ChildView(result.getObject("id", UUID.class), result.getString("name"),
                    result.getString("nickname"), result.getObject("birth_date", LocalDate.class),
                    ChildView.Gender.valueOf(result.getString("gender")), List.of((String[]) interests.getArray()),
                    result.getString("character_id"), result.getObject("created_at", OffsetDateTime.class)
                    .withOffsetSameInstant(ZoneOffset.ofHours(9)).truncatedTo(ChronoUnit.SECONDS));
        }
        finally {
            interests.free();
        }
    };

    private final JdbcTemplate jdbc;

    /** 기존 JDBC 연결과 예외 변환을 사용한다. */
    public ChildRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 정규화된 프로필을 저장하며 계정당 중복만 빈 결과로 반환한다. */
    public Optional<ChildView> insert(UUID accountId, CreateChildRequest normalized) {
        try {
            return jdbc.query("""
                    INSERT INTO children (id, account_id, name, nickname, birth_date, gender, interests, character_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (account_id) DO NOTHING
                    RETURNING
                    """ + COLUMNS, CHILD_ROW, UUID.randomUUID(), accountId, normalized.name(), normalized.nickname(),
                    normalized.birthDate(), normalized.gender().name(),
                    new SqlArrayValue("text", normalized.interests().toArray()), normalized.characterId())
                    .stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ChildException(failure);
        }
    }

    /** 계정과 자녀를 함께 조회해 미소유·미존재를 같은 빈 결과로 처리한다. */
    public Optional<ChildView> findOwned(UUID accountId, UUID childId) {
        try {
            return jdbc.query("SELECT " + COLUMNS + " FROM children WHERE id = ? AND account_id = ?",
                    CHILD_ROW, childId, accountId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ChildException(failure);
        }
    }

    /** 선행 소유권 검사를 마친 내부 문맥 조회에 사용할 프로필을 읽는다. */
    public Optional<ChildView> findById(UUID childId) {
        try {
            return jdbc.query("SELECT " + COLUMNS + " FROM children WHERE id = ?", CHILD_ROW, childId)
                    .stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ChildException(failure);
        }
    }
}
