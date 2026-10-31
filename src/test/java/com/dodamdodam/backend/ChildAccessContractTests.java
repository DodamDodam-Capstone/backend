package com.dodamdodam.backend;

import com.dodamdodam.backend.child.ChildException;
import com.dodamdodam.backend.child.ChildService;
import com.dodamdodam.backend.child.ChildView;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ChildAccessContractTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChildService service;
    @Autowired private JsonMapper jsonMapper;

    @Test
    void ownerCanReadChild() {
        var owner = account();
        var childId = child(owner);
        child(account());

        var view = service.requireChildAccess(owner, childId);

        assertThat(view.childId()).isEqualTo(childId);
        assertThat(view.name()).isEqualTo("가나");
        assertThat(view.nickname()).isEqualTo("별이");
        assertThat(view.birthDate()).isEqualTo(LocalDate.of(2020, 5, 12));
        assertThat(view.gender()).isEqualTo(ChildView.Gender.FEMALE);
        assertThat(view.interests()).containsExactly("공룡", "그림");
        assertThat(view.characterId()).isEqualTo("test-character");
        assertThat(view.createdAt()).isEqualTo(OffsetDateTime.parse("2026-10-01T11:03:04+09:00"));
        assertNoAuthenticationData(view, 8);
    }

    @Test
    void otherOwnerAndMissingChildHaveSame404() {
        var owner = account();
        var other = account();
        var childId = child(owner);

        var unowned = assertThrows(ChildException.class, () -> service.requireChildAccess(other, childId));
        var missing = assertThrows(ChildException.class, () -> service.requireChildAccess(owner, UUID.randomUUID()));

        assertThat(unowned.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(missing.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(unowned.getMessage()).isNotBlank().isEqualTo(missing.getMessage())
                .doesNotContain(owner.toString(), other.toString(), childId.toString(), "가나", "별이");
        assertThat(unowned.fields()).isEmpty();
        assertThat(missing.fields()).isEmpty();
        assertThat(unowned.getCause()).isNull();
        assertThat(missing.getCause()).isNull();
    }

    @Test
    void contextPreservesNicknameAndInterests() {
        var owner = account();
        var childId = child(owner);
        service.requireChildAccess(owner, childId);

        var context = service.getChildContext(childId);

        assertThat(context.childId()).isEqualTo(childId);
        assertThat(context.name()).isEqualTo("가나");
        assertThat(context.nickname()).isEqualTo("별이");
        assertThat(context.birthDate()).isEqualTo(LocalDate.of(2020, 5, 12));
        assertThat(context.interests()).containsExactly("공룡", "그림");
        assertThat(context.characterId()).isEqualTo("test-character");
        assertThat(context.createdAt()).isEqualTo(OffsetDateTime.parse("2026-10-01T11:03:04+09:00"));
        assertNoAuthenticationData(context, 7);
    }

    @Test
    void contextAllowsNoInterests() {
        var owner = account();
        var childId = child(owner);
        jdbc.update("UPDATE children SET interests = ARRAY[]::text[] WHERE id = ?", childId);
        service.requireChildAccess(owner, childId);

        assertThat(service.getChildContext(childId).interests()).isEmpty();
    }

    @Test
    void missingChildContextIsNotFound() {
        var failure = assertThrows(ChildException.class, () -> service.getChildContext(UUID.randomUUID()));

        assertThat(failure.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(failure.fields()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ID", "OWNER"})
    void storageFailureIsNotReportedAsMissing(String lookup) {
        jdbc.execute("ALTER TABLE children RENAME TO unavailable_children");

        assertThatThrownBy(() -> {
            if (lookup.equals("ID")) service.getChildContext(UUID.randomUUID());
            else service.requireChildAccess(UUID.randomUUID(), UUID.randomUUID());
        }).isInstanceOfSatisfying(ChildException.class, failure -> {
            assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(failure.getCause()).isInstanceOf(DataAccessException.class);
        });
    }

    private void assertNoAuthenticationData(Object value, int fieldCount) {
        var json = jsonMapper.readTree(jsonMapper.writeValueAsString(value));
        assertThat(json.size()).isEqualTo(fieldCount);
        assertThat(json.has("accountId")).isFalse();
        assertThat(json.has("sessionId")).isFalse();
        assertThat(json.has("securityContextId")).isFalse();
    }

    private UUID account() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", id, id + "@example.invalid");
        return id;
    }

    private UUID child(UUID accountId) {
        var id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, interests,
                                      character_id, created_at)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', ARRAY['공룡', '그림'],
                        'test-character', TIMESTAMPTZ '2026-10-01 02:03:04.123456+00')
                """, id, accountId);
        return id;
    }
}
