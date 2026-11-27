package com.allforland.orda.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/** 챗봇 요청. 서버는 대화를 저장하지 않으므로 화면이 지금까지의 대화를 함께 보낸다. */
@Getter
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
public class ChatRequest {
    /** 오래된 순서의 대화. 사용자와 챗봇이 번갈아 나오고 마지막은 사용자 메시지다. */
    private List<ChatMessage> messages;
}
