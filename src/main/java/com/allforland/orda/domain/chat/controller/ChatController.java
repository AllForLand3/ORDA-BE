package com.allforland.orda.domain.chat.controller;

import com.allforland.orda.domain.chat.dto.ChatRequest;
import com.allforland.orda.domain.chat.dto.ChatResponse;
import com.allforland.orda.domain.chat.service.ChatService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 서비스 이용 안내 챗봇 HTTP 요청을 처리한다. */
@RestController
@RequestMapping("/chat")
public class ChatController {
    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    /** 지금까지의 대화를 받아 서비스 위키를 근거로 다음 답변을 만든다. */
    @PostMapping("/messages")
    public ResponseEntity<ChatResponse> reply(@RequestBody ChatRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.reply(request));
    }
}
