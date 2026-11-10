package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Giới hạn số tin nhắn mỗi phút. /chatbot/message là endpoint công khai và mỗi tin tốn quota Gemini.
 * Đếm theo từng instance server, đủ cho quy mô hiện tại.
 */
@Component
public class ChatbotRateLimiter {

    /** Đủ rộng cho nút "thêm toàn bộ", vốn gửi mỗi món một request. */
    static final int USER_LIMIT_PER_MINUTE = 40;
    static final int GUEST_LIMIT_PER_MINUTE = 15;
    private static final long WINDOW_MILLIS = 60_000;

    private record Window(long startedAt, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public ChatbotRateLimiter() {
        this(System::currentTimeMillis);
    }

    ChatbotRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    public void check(Integer userId, String clientIp) {
        long now = clock.getAsLong();
        String key = userId != null ? "user:" + userId : "ip:" + clientIp;
        int limit = userId != null ? USER_LIMIT_PER_MINUTE : GUEST_LIMIT_PER_MINUTE;

        Window window = windows.compute(key, (k, current) ->
                current == null || now - current.startedAt() >= WINDOW_MILLIS
                        ? new Window(now, 1)
                        : new Window(current.startedAt(), current.count() + 1));

        if (window.count() > limit) {
            throw new AppException(ErrorCode.CHATBOT_RATE_LIMITED);
        }
        if (windows.size() > 10_000) {
            windows.values().removeIf(w -> now - w.startedAt() >= WINDOW_MILLIS);
        }
    }
}
