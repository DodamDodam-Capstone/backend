package com.dodamdodam.backend.conversation.dto;

import java.util.List;

/** 종료 대화와 현재 목록 페이지의 탐색 정보를 담는 공개 응답이다. */
public record ConversationPageView(List<ConversationView> items, long page, int size, boolean hasNext) {
    public ConversationPageView {
        items = List.copyOf(items);
    }
}
