package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.conversation.domain.TextVisibility;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedTextPolicyTests {
    private final AllowedTextPolicy policy = new AllowedTextPolicy();

    @ParameterizedTest
    @MethodSource("permissionCases")
    void onlyExplicitPermissionForBothStorageAndDisplayAllowsText(
            TextVisibility visibility, Boolean storageAllowed, Boolean displayAllowed, String text) {
        var result = policy.select(visibility, storageAllowed, displayAllowed, text);
        boolean allowed = (visibility == TextVisibility.VISIBLE || visibility == TextVisibility.REDACTED)
                && Boolean.TRUE.equals(storageAllowed) && Boolean.TRUE.equals(displayAllowed) && text != null;

        assertThat(result.visibility()).isEqualTo(allowed ? visibility : TextVisibility.OMITTED);
        assertThat(result.text()).isEqualTo(allowed ? text : null);
    }

    private static Stream<Arguments> permissionCases() {
        TextVisibility[] visibilities = {TextVisibility.VISIBLE, TextVisibility.REDACTED, TextVisibility.OMITTED, null};
        Boolean[] permissions = {true, false, null};
        String[] texts = {"synthetic candidate text", null};
        return Arrays.stream(visibilities).flatMap(visibility ->
                Arrays.stream(permissions).flatMap(storage ->
                        Arrays.stream(permissions).flatMap(display ->
                                Arrays.stream(texts).map(text -> Arguments.of(visibility, storage, display, text)))));
    }

    @Test
    void omittedChildDoesNotSuppressVisibleReply() {
        var child = policy.select(TextVisibility.VISIBLE, false, true, "synthetic rejected child");
        var reply = policy.select(TextVisibility.VISIBLE, true, true, "synthetic permitted reply");

        assertThat(child.visibility()).isEqualTo(TextVisibility.OMITTED);
        assertThat(child.text()).isNull();
        assertThat(reply.visibility()).isEqualTo(TextVisibility.VISIBLE);
        assertThat(reply.text()).isEqualTo("synthetic permitted reply");
    }

    @Test
    void omittedReplyDoesNotSuppressVisibleChildOrChangePreviousResult() {
        var child = policy.select(TextVisibility.VISIBLE, true, true, "synthetic permitted child");
        var reply = policy.select(TextVisibility.REDACTED, true, null, "synthetic unconfirmed reply");

        assertThat(child.visibility()).isEqualTo(TextVisibility.VISIBLE);
        assertThat(child.text()).isEqualTo("synthetic permitted child");
        assertThat(reply.visibility()).isEqualTo(TextVisibility.OMITTED);
        assertThat(reply.text()).isNull();
    }

    @Test
    void childAndReplyCanUseDifferentPermittedRepresentations() {
        var child = policy.select(TextVisibility.REDACTED, true, true, "synthetic processed child");
        var reply = policy.select(TextVisibility.VISIBLE, true, true, "synthetic original reply");

        assertThat(child.visibility()).isEqualTo(TextVisibility.REDACTED);
        assertThat(child.text()).isEqualTo("synthetic processed child");
        assertThat(reply.visibility()).isEqualTo(TextVisibility.VISIBLE);
        assertThat(reply.text()).isEqualTo("synthetic original reply");
    }

    @Test
    void missingProcessedTextIsOmittedWithoutGeneratingReplacement() {
        var result = policy.select(TextVisibility.REDACTED, true, true, null);

        assertThat(result.visibility()).isEqualTo(TextVisibility.OMITTED);
        assertThat(result.text()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  synthetic permitted text  ", "synthetic\npermitted text"})
    void permittedTextIsPreservedWithoutInventingNormalizationOrLengthRules(String text) {
        var result = policy.select(TextVisibility.REDACTED, true, true, text);

        assertThat(result.visibility()).isEqualTo(TextVisibility.REDACTED);
        assertThat(result.text()).isSameAs(text);
    }

    @Test
    void diagnosticOutputDoesNotIncludeText() {
        var permitted = policy.select(TextVisibility.VISIBLE, true, true, "synthetic permitted original");
        var omitted = policy.select(TextVisibility.VISIBLE, false, true, "synthetic forbidden original");

        assertThat(permitted.toString()).isEqualTo("AllowedText[visibility=VISIBLE]");
        assertThat(omitted.toString()).isEqualTo("AllowedText[visibility=OMITTED]");
    }
}
