package com.dodamdodam.backend.conversation.application;

import com.dodamdodam.backend.conversation.domain.TextVisibility;

public final class AllowedTextPolicy {

    /** 각 텍스트에 독립 적용하며, REDACTED에는 이미 명시적으로 허용된 가공문만 전달한다. */
    public AllowedText select(TextVisibility visibility, Boolean storageAllowed,
                              Boolean displayAllowed, String permittedText) {
        if (visibility == null || visibility == TextVisibility.OMITTED
                || !Boolean.TRUE.equals(storageAllowed) || !Boolean.TRUE.equals(displayAllowed)
                || permittedText == null) {
            return new AllowedText(TextVisibility.OMITTED, null);
        }
        return new AllowedText(visibility, permittedText);
    }

    /**
     * 결과 객체는 정책 클래스 내부에서만 생성할 수 있습니다.
     * 허용되지 않은 텍스트는 결과 객체에 보관하지 않습니다.
     */
    public static final class AllowedText {
        private final TextVisibility visibility;
        private final String text;

        private AllowedText(TextVisibility visibility, String text) {
            this.visibility = visibility;
            this.text = text;
        }

        public TextVisibility visibility() {
            return visibility;
        }

        public String text() {
            return text;
        }

        @Override
        public String toString() {
            return "AllowedText[visibility=" + visibility + "]";
        }
    }
}
