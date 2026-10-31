package com.dodamdodam.backend;

import com.dodamdodam.backend.child.ChildException;
import com.dodamdodam.backend.child.ChildRepository;
import com.dodamdodam.backend.child.ChildView;
import com.dodamdodam.backend.child.CreateChildRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.session.jdbc.cleanup-cron=-"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ChildRepositoryTests {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private ChildRepository repository;
    @Autowired private JsonMapper jsonMapper;

    @Test
    void storesSeparateNameAndNickname() {
        var accountId = account();

        var child = repository.insert(accountId, profile(List.of())).orElseThrow();

        assertThat(child.name()).isEqualTo("가나");
        assertThat(child.nickname()).isEqualTo("별이");
        assertThat(child.birthDate()).isEqualTo(LocalDate.of(2020, 5, 12));
        assertThat(child.gender()).isEqualTo(ChildView.Gender.FEMALE);
        assertThat(child.characterId()).isEqualTo("test-character");
        assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, child.childId()))
                .isEqualTo(accountId);
    }

    @Test
    void rejectsSecondChildForSameAccount() {
        var accountId = account();
        var first = repository.insert(accountId, profile(List.of())).orElseThrow();
        var otherProfile = new CreateChildRequest("다라", "달이", LocalDate.of(2021, 6, 13),
                ChildView.Gender.MALE, List.of("우주"), "other-character");

        assertThat(repository.insert(accountId, otherProfile)).isEmpty();
        assertThat(repository.findById(first.childId())).contains(first);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM children WHERE account_id = ?", Integer.class, accountId))
                .isEqualTo(1);
    }

    @Test
    void permitsSameProfileAcrossAccounts() {
        var first = repository.insert(account(), profile(List.of())).orElseThrow();
        var second = repository.insert(account(), profile(List.of())).orElseThrow();

        assertThat(first.childId()).isNotEqualTo(second.childId());
        assertThat(second.name()).isEqualTo("가나");
        assertThat(second.nickname()).isEqualTo("별이");
    }

    @Test
    void keepsInterestOrderAndEmptyArray() {
        var ordered = repository.insert(account(), profile(List.of("그림", "공룡"))).orElseThrow();
        var empty = repository.insert(account(), profile(List.of())).orElseThrow();

        assertThat(ordered.interests()).containsExactly("그림", "공룡");
        assertThat(repository.findById(ordered.childId()).orElseThrow().interests()).containsExactly("그림", "공룡");
        assertThat(empty.interests()).isEmpty();
        assertThat(repository.findById(empty.childId()).orElseThrow().interests()).isEmpty();
    }

    @Test
    void preservesArrayEscapesWithoutSqlInterpolation() {
        var child = repository.insert(account(), profile(List.of("별,달", "\"따옴표\"", "역\\슬래시", "😀")))
                .orElseThrow();

        assertThat(repository.findById(child.childId()).orElseThrow().interests())
                .containsExactly("별,달", "\"따옴표\"", "역\\슬래시", "😀");
    }

    @Test
    void rejectsMissingAccount() {
        assertThatThrownBy(() -> repository.insert(UUID.randomUUID(), profile(List.of())))
                .isInstanceOfSatisfying(ChildException.class, failure -> {
                    assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
                    assertThat(failure.fields()).isEmpty();
                    assertThat(failure.getCause()).isInstanceOf(DataIntegrityViolationException.class);
                    assertThat(failure.getMessage()).doesNotContain("INSERT", "test-character");
                });
    }

    @Test
    void otherConstraintFailuresAreNotChildLimitConflicts() {
        var invalid = new CreateChildRequest("가나다라마바", "별이", LocalDate.of(2020, 5, 12),
                ChildView.Gender.FEMALE, List.of(), "test-character");

        assertThatThrownBy(() -> repository.insert(account(), invalid))
                .isInstanceOf(ChildException.class).hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsOnlyTheRequestedChildAndOwner() {
        var owner = account();
        var other = account();
        var child = repository.insert(owner, profile(List.of())).orElseThrow();

        assertThat(repository.findOwned(owner, child.childId())).contains(child);
        assertThat(repository.findOwned(other, child.childId())).isEmpty();
        assertThat(repository.findOwned(owner, UUID.randomUUID())).isEmpty();
        assertThat(repository.findById(child.childId())).contains(child);
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void createdAtUsesKstSecondsWithoutChangingStoredTimestamp() throws Exception {
        var childId = storedChild(account());
        jdbc.update("UPDATE children SET created_at = TIMESTAMPTZ '2026-09-30 05:00:00.123456+00' WHERE id = ?", childId);

        var child = repository.findById(childId).orElseThrow();
        var json = jsonMapper.readTree(jsonMapper.writeValueAsString(child));

        assertThat(child.createdAt()).isEqualTo(OffsetDateTime.parse("2026-09-30T14:00:00+09:00"));
        assertThat(child.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
        assertThat(json.get("createdAt").stringValue()).isEqualTo("2026-09-30T14:00:00+09:00");
        assertThat(json.get("nickname").stringValue()).isEqualTo("별이");
        assertThat(json.get("birthDate").stringValue()).isEqualTo("2020-05-12");
        assertThat(json.size()).isEqualTo(8);
        assertThat(json.has("accountId")).isFalse();
        var stored = jdbc.queryForObject("SELECT created_at FROM children WHERE id = ?",
                (result, row) -> result.getObject(1, OffsetDateTime.class), childId);
        assertThat(stored.toInstant()).isEqualTo(OffsetDateTime.parse("2026-09-30T05:00:00.123456Z").toInstant());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ID", "OWNER"})
    void lookupFailureRetainsItsStorageCause(String lookup) {
        jdbc.execute("ALTER TABLE children RENAME TO unavailable_children");

        assertThatThrownBy(() -> {
            if (lookup.equals("ID")) repository.findById(UUID.randomUUID());
            else repository.findOwned(UUID.randomUUID(), UUID.randomUUID());
        }).isInstanceOfSatisfying(ChildException.class, failure -> {
            assertThat(failure.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(failure.getCause()).isInstanceOf(org.springframework.dao.DataAccessException.class);
        });
    }

    @Test
    void migrationCreatesChildrenWithOwnershipConstraints() {
        assertThat(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM information_schema.tables
                               WHERE table_schema = 'public' AND table_name = 'children')
                """, Boolean.class)).isTrue();

        var accountId = account();
        var childId = storedChild(accountId);
        assertThat(jdbc.queryForObject("SELECT account_id FROM children WHERE id = ?", UUID.class, childId))
                .isEqualTo(accountId);
        assertThat(jdbc.queryForObject("SELECT cardinality(interests) FROM children WHERE id = ?", Integer.class, childId))
                .isZero();
    }

    @Test
    void databaseRejectsSecondChildForSameAccount() {
        var accountId = account();
        storedChild(accountId);

        assertThatThrownBy(() -> storedChild(accountId)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsMissingAccount() {
        assertThatThrownBy(() -> storedChild(UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void childOwnershipPreventsAccountDeletion() {
        var accountId = account();
        storedChild(accountId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM accounts WHERE id = ?", accountId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name = NULL", "name = ''", "name = ' 가나다'", "name = '가나다 '", "name = '가나다라마바'",
            "nickname = NULL", "nickname = ''", "nickname = ' 별이'", "nickname = '별이 '",
            "nickname = repeat('가', 21)", "birth_date = NULL", "birth_date = 'infinity'::date",
            "gender = NULL", "gender = 'UNSPECIFIED'", "gender = 'FEMALE' || chr(10)",
            "character_id = NULL", "character_id = ''", "character_id = repeat('a', 65)",
            "character_id = '캐릭터'", "character_id = 'test-character' || chr(10)",
            "interests = NULL", "interests = ARRAY[NULL]::text[]",
            "interests = array_fill('놀이'::text, ARRAY[11])",
            "interests = ARRAY[['공룡','그림'],['우주','과학']]",
            "created_at = NULL", "created_at = '-infinity'::timestamptz"
    })
    void databaseRejectsInvalidStoredProfile(String assignment) {
        var childId = storedChild(account());

        assertThatThrownBy(() -> jdbc.update("UPDATE children SET " + assignment + " WHERE id = ?", childId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseLengthsCountUnicodeCodePoints() {
        var childId = storedChild(account());

        jdbc.update("UPDATE children SET name = ?, nickname = ? WHERE id = ?",
                "😀😀😀😀😀", "😀".repeat(20), childId);

        assertThat(jdbc.queryForObject("SELECT char_length(name) FROM children WHERE id = ?", Integer.class, childId))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT char_length(nickname) FROM children WHERE id = ?", Integer.class, childId))
                .isEqualTo(20);
    }

    @Test
    void databaseAcceptsMaximumArrayAndCharacterLengths() {
        var childId = storedChild(account());

        jdbc.update("""
                UPDATE children SET character_id = repeat('a', 64),
                    interests = ARRAY['a','b','c','d','e','f','g','h','i','j']
                WHERE id = ?
                """, childId);

        var child = repository.findById(childId).orElseThrow();
        assertThat(child.characterId()).hasSize(64);
        assertThat(child.interests()).containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
    }

    private UUID account() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO accounts (id, email) VALUES (?, ?)", id, id + "@example.invalid");
        return id;
    }

    private CreateChildRequest profile(List<String> interests) {
        return new CreateChildRequest("가나", "별이", LocalDate.of(2020, 5, 12),
                ChildView.Gender.FEMALE, interests, "test-character");
    }

    private UUID storedChild(UUID accountId) {
        var childId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO children (id, account_id, name, nickname, birth_date, gender, character_id)
                VALUES (?, ?, '가나', '별이', DATE '2020-05-12', 'FEMALE', 'test-character')
                """, childId, accountId);
        return childId;
    }
}
