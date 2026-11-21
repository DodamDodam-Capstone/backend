package com.dodamdodam.backend.conversation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** 종료된 대화의 메타정보와 현재 발화 페이지를 담고 마지막 페이지의 null 커서를 보존한다. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ConversationDetailView(
        ConversationView conversation,
        List<TurnView> turns,
        boolean hasNext,
        Integer nextAfterSequence
) { }
