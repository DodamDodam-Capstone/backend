package com.dodamdodam.backend.child;

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

    public ChildRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

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

    public Optional<ChildView> findOwned(UUID accountId, UUID childId) {
        try {
            return jdbc.query("SELECT " + COLUMNS + " FROM children WHERE id = ? AND account_id = ?",
                    CHILD_ROW, childId, accountId).stream().findFirst();
        }
        catch (DataAccessException failure) {
            throw new ChildException(failure);
        }
    }

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
