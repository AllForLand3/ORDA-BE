package com.allforland.orda.domain.chat.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 사용자별 챗봇 요청 수를 제한한다. 서버 메모리에만 두므로 재시작하면 초기화된다. */
@Component
public class ChatRateLimiter {
    private final int maxRequests;
    private final long windowMillis;
    private final Map<Long, Deque<Long>> requests = new ConcurrentHashMap<>();

    public ChatRateLimiter(
            @Value("${orda.chat.rate-limit.max-requests:20}") int maxRequests,
            @Value("${orda.chat.rate-limit.window-minutes:10}") long windowMinutes) {
        if (maxRequests <= 0 || windowMinutes <= 0) {
            throw new IllegalArgumentException("orda.chat.rate-limit values must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = windowMinutes * 60_000L;
    }

    /** 허용 범위 안이면 요청을 기록하고 true를 반환한다. */
    public boolean tryAcquire(long userId) {
        long now = System.currentTimeMillis();
        Deque<Long> times = requests.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() >= windowMillis) {
                times.pollFirst();
            }
            if (times.size() >= maxRequests) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }
}
