package com.dodamdodam.backend.conversation.dto;

import com.dodamdodam.backend.conversation.exception.ConversationException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** KST 시작 날짜 범위, 페이지 경계와 정규화한 검색어를 담는다. */
public record GuardianConversationListRequest(LocalDate from, LocalDate to, long page, int size, long offset,
                                              String keyword) {
    private static final Pattern FULL_DATE = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final Pattern NONNEGATIVE_INTEGER = Pattern.compile("[0-9]+");

    /** 생략한 날짜·페이지에만 기본값을 적용하고 PostgreSQL OFFSET 범위를 검사한다. */
    public static GuardianConversationListRequest fromQuery(String from, String to, String page, String size, String q) {
        var fromDate = parseDate(from, "from");
        var toDate = parseDate(to, "to");
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw ConversationException.validation("to");
        }

        long pageNumber = parseNumber(page, "page", 0);
        long pageSize = parseNumber(size, "size", 20);
        if (pageSize < 1 || pageSize > 100) {
            throw ConversationException.validation("size");
        }
        var keyword = normalizeKeyword(q);
        try {
            return new GuardianConversationListRequest(fromDate, toDate, pageNumber, (int) pageSize,
                    Math.multiplyExact(pageNumber, pageSize), keyword);
        }
        catch (ArithmeticException overflow) {
            throw ConversationException.validation("page");
        }
    }

    /** 검색어 양끝만 정리하고 Unicode 문자 기준으로 공개 명세의 길이를 검사한다. */
    private static String normalizeKeyword(String query) {
        if (query == null) {
            return null;
        }
        var normalized = query.strip();
        if (normalized.codePointCount(0, normalized.length()) > 100) {
            throw ConversationException.validation("q");
        }
        return normalized.isEmpty() ? null : normalized;
    }

    /** API의 네 자리 연도 날짜만 허용해 다음날 KST 경계가 지원 범위를 벗어나지 않게 한다. */
    private static LocalDate parseDate(String value, String field) {
        if (value == null) {
            return null;
        }
        if (!FULL_DATE.matcher(value).matches()) {
            throw ConversationException.validation(field);
        }
        try {
            return LocalDate.parse(value);
        }
        catch (DateTimeParseException malformed) {
            throw ConversationException.validation(field);
        }
    }

    /** 소수·음수·공백을 거부하고 실제 숫자가 signed bigint 범위 안인지 확인한다. */
    private static long parseNumber(String value, String field, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (!NONNEGATIVE_INTEGER.matcher(value).matches()) {
            throw ConversationException.validation(field);
        }
        try {
            return Long.parseLong(value);
        }
        catch (NumberFormatException overflow) {
            throw ConversationException.validation(field);
        }
    }
}
