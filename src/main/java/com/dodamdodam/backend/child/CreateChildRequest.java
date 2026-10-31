package com.dodamdodam.backend.child;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

public record CreateChildRequest(String name, String nickname, LocalDate birthDate, ChildView.Gender gender,
                                 List<String> interests, String characterId) {

    private static final Set<String> FIELDS = Set.of("name", "nickname", "birthDate", "gender", "interests", "characterId");
    private static final Pattern DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final Pattern CHARACTER_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    /** JSON 구조와 타입을 검사해 요청을 만들며, 값 정규화는 normalized에서 수행한다. */
    public static CreateChildRequest fromJson(JsonNode body) {
        // 세션 계정은 서버에서 결정하므로 accountId 등 정의되지 않은 필드를 거부한다.
        if (body == null || !body.isObject() || !FIELDS.containsAll(body.propertyNames())) {
            throw ChildException.validation("body");
        }
        var name = requiredString(body.get("name"), "name");
        var nickname = requiredString(body.get("nickname"), "nickname");
        var date = requiredString(body.get("birthDate"), "birthDate");
        // 날짜 형식과 실제 존재 여부를 모두 검사하며 공백·개행을 보정하지 않는다.
        LocalDate birthDate;
        try {
            if (!DATE.matcher(date).matches()) {
                throw ChildException.validation("birthDate");
            }
            birthDate = LocalDate.parse(date);
        }
        catch (DateTimeParseException invalid) {
            throw ChildException.validation("birthDate");
        }
        // enum 이름과 정확히 일치해야 하므로 대소문자나 바깥 공백을 보정하지 않는다.
        ChildView.Gender gender;
        try {
            gender = ChildView.Gender.valueOf(requiredString(body.get("gender"), "gender"));
        }
        catch (IllegalArgumentException invalid) {
            throw ChildException.validation("gender");
        }
        // 생략은 빈 목록으로 처리하고, 명시적 null과 문자열이 아닌 원소는 거부한다.
        var interests = new ArrayList<String>();
        var supplied = body.get("interests");
        if (supplied != null) {
            if (!supplied.isArray() || supplied.size() > 10) {
                throw ChildException.validation("interests");
            }
            for (int i = 0; i < supplied.size(); i++) {
                interests.add(requiredString(supplied.get(i), "interests"));
            }
        }
        return new CreateChildRequest(name, nickname, birthDate, gender, List.copyOf(interests),
                requiredString(body.get("characterId"), "characterId"));
    }

    /** 생성자로 직접 만든 요청도 검증하며, 저장에 사용할 정규화된 새 객체를 반환한다. */
    public CreateChildRequest normalized(Clock clock, Set<String> supportedCharacterIds) {
        var normalizedName = normalizedText(name, 5, "name");
        var normalizedNickname = normalizedText(nickname, 20, "nickname");
        // 서버 기본 시간대와 무관하게 한국 날짜를 기준으로 미래 생일을 거부한다.
        if (birthDate == null || birthDate.isAfter(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))) {
            throw ChildException.validation("birthDate");
        }
        if (gender == null) {
            throw ChildException.validation("gender");
        }
        if (interests == null || interests.size() > 10) {
            throw ChildException.validation("interests");
        }
        // 중복은 공백 제거 후 판정하고, 결과 목록에는 원래 입력 순서를 유지한다.
        var normalizedInterests = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (var interest : interests) {
            var normalized = normalizedText(interest, 30, "interests");
            if (!seen.add(normalized)) {
                throw ChildException.validation("interests");
            }
            normalizedInterests.add(normalized);
        }
        // 캐릭터 ID는 공백 보정 없이 형식과 지원 목록을 모두 만족해야 한다.
        if (characterId == null || !CHARACTER_ID.matcher(characterId).matches()
                || !supportedCharacterIds.contains(characterId)) {
            throw ChildException.validation("characterId");
        }
        // 원본 요청을 유지하고, 정규화한 관심사 목록의 이후 변경도 막는다.
        return new CreateChildRequest(normalizedName, normalizedNickname, birthDate, gender,
                List.copyOf(normalizedInterests), characterId);
    }

    /** 필수 JSON 문자열을 읽고 누락·null·타입 오류를 해당 필드의 검증 예외로 처리한다. */
    private static String requiredString(JsonNode node, String field) {
        // 숫자·불리언 등을 문자열로 자동 변환하지 않아 요청 타입 계약을 지킨다.
        if (node == null || !node.isString()) {
            throw ChildException.validation(field);
        }
        return node.stringValue();
    }

    /** 바깥 Unicode 공백을 제거하고 코드포인트 길이와 저장 가능한 문자 여부를 검사한다. */
    private static String normalizedText(String value, int maximum, String field) {
        if (value == null) {
            throw ChildException.validation(field);
        }
        // 양끝 경계만 이동해 내부 공백을 보존하고, 코드포인트 단위로 이동해 문자를 분리하지 않는다.
        int start = 0;
        int end = value.length();
        while (start < end && isSpace(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && isSpace(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        var trimmed = value.substring(start, end);
        // 보충 평면 문자도 한 코드포인트로 세어 이름·애칭·관심사의 길이 제한을 적용한다.
        int codePointLength = trimmed.codePointCount(0, trimmed.length());
        if (codePointLength < 1 || codePointLength > maximum) {
            throw ChildException.validation(field);
        }
        // PostgreSQL text의 NUL 오류와 잘못된 surrogate의 UTF-8 변환 손실을 막는다.
        if (trimmed.codePoints().anyMatch(codePoint -> codePoint == 0
                || (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE))) {
            throw ChildException.validation(field);
        }
        return trimmed;
    }

    /** 양끝 공백 제거에 사용할 Unicode 공백 기준이다. */
    private static boolean isSpace(int codePoint) {
        // isWhitespace에서 제외되는 NBSP 같은 공백도 포함한다.
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}
