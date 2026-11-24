package com.allforland.orda.domain.chat.service;

import com.allforland.orda.common.error.BusinessException;
import com.allforland.orda.common.error.ErrorCode;
import com.allforland.orda.domain.auth.service.AuthService;
import com.allforland.orda.domain.chat.dto.ChatMessage;
import com.allforland.orda.domain.chat.dto.ChatRequest;
import com.allforland.orda.domain.chat.dto.ChatResponse;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.models.beta.AnthropicBeta;
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.BetaTextBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 서비스 위키를 근거로 이용 안내 질문에 답한다. 대화는 저장하지 않는다. */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_MESSAGES = 20;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String REFUSAL_REPLY = "이 질문에는 답변하기 어려워요. ORDA 이용 방법에 대해 다시 질문해주세요.";

    private final AuthService auth;
    private final ChatKnowledge knowledge;
    private final ChatRateLimiter rateLimiter;
    private final String model;
    private final long maxTokens;
    private final AnthropicClient client;

    public ChatService(
            AuthService auth,
            ChatKnowledge knowledge,
            ChatRateLimiter rateLimiter,
            @Value("${orda.chat.model:claude-opus-5-5}") String model,
            @Value("${orda.chat.max-tokens:4096}") long maxTokens,
            @Value("${orda.chat.api-key:}") String apiKey) {
        if (model == null || model.isBlank() || maxTokens <= 0) {
            throw new IllegalArgumentException(
                    "orda.chat.model and orda.chat.max-tokens are required");
        }
        this.auth = auth;
        this.knowledge = knowledge;
        this.rateLimiter = rateLimiter;
        this.model = model;
        this.maxTokens = maxTokens;
        // 키가 없어도 서버는 뜨고, 챗봇 요청만 CHAT_NOT_CONFIGURED로 응답한다.
        this.client =
                apiKey == null || apiKey.isBlank()
                        ? null
                        : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    /** 대화를 검증하고 다음 답변을 반환한다. */
    public ChatResponse reply(ChatRequest request) {
        long userId = auth.requireCurrentUser().userId();
        List<ChatMessage> messages = validate(request);
        if (client == null) {
            throw new BusinessException(ErrorCode.CHAT_NOT_CONFIGURED);
        }
        if (!rateLimiter.tryAcquire(userId)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        BetaMessage response;
        try {
            response = client.beta().messages().create(params(messages));
        } catch (RateLimitException exception) {
            log.warn("Chat model rate limited");
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        } catch (AnthropicException exception) {
            log.warn("Chat model request failed: {}", exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.CHAT_UNAVAILABLE);
        }

        Optional<BetaStopReason> stopReason = response.stopReason();
        if (stopReason.isPresent() && BetaStopReason.REFUSAL.equals(stopReason.get())) {
            return new ChatResponse(REFUSAL_REPLY);
        }
        String reply =
                response.content().stream()
                        .map(BetaContentBlock::text)
                        .flatMap(Optional::stream)
                        .map(BetaTextBlock::text)
                        .collect(Collectors.joining())
                        .strip();
        if (reply.isEmpty()) {
            throw new BusinessException(ErrorCode.CHAT_UNAVAILABLE);
        }
        return new ChatResponse(reply);
    }

    private MessageCreateParams params(List<ChatMessage> messages) {
        MessageCreateParams.Builder params =
                MessageCreateParams.builder()
                        .model(model)
                        .maxTokens(maxTokens)
                        // 위키 전체가 들어가는 시스템 프롬프트는 바뀌지 않으므로 캐시한다.
                        .systemOfBetaTextBlockParams(
                                List.of(
                                        BetaTextBlockParam.builder()
                                                .text(knowledge.systemPrompt())
                                                .cacheControl(
                                                        BetaCacheControlEphemeral.builder().build())
                                                .build()))
                        // 이용 안내는 짧은 답이라 낮은 effort로 충분하다.
                        .outputConfig(
                                BetaOutputConfig.builder()
                                        .effort(BetaOutputConfig.Effort.LOW)
                                        .build())
                        // 안전 분류기가 거절하면 서버가 거절 유형에 맞는 모델로 다시 시도한다.
                        .addBeta(AnthropicBeta.SERVER_SIDE_FALLBACK_2026_07_01)
                        .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        for (ChatMessage message : messages) {
            if ("user".equals(message.role())) {
                params.addUserMessage(message.content());
            } else {
                params.addAssistantMessage(message.content());
            }
        }
        return params.build();
    }

    /** 사용자와 챗봇이 번갈아 나오고 마지막이 사용자인 대화만 받는다. */
    private static List<ChatMessage> validate(ChatRequest request) {
        if (request == null || request.messages() == null || request.messages().isEmpty()) {
            throw invalidRequest();
        }
        List<ChatMessage> messages = request.messages();
        if (messages.size() > MAX_MESSAGES || messages.size() % 2 == 0) {
            throw invalidRequest();
        }
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            String expectedRole = i % 2 == 0 ? "user" : "assistant";
            if (message == null
                    || !expectedRole.equals(message.role())
                    || message.content() == null
                    || message.content().isBlank()
                    || message.content().length() > MAX_MESSAGE_LENGTH) {
                throw invalidRequest();
            }
        }
        return messages;
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }
}
