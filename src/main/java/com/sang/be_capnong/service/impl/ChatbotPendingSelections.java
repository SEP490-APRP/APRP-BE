package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.response.ChatbotMessageResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Danh sách bot vừa đưa ra cho từng tài khoản, để khách trả lời "lấy cái đầu tiên".
 * Lưu trong bộ nhớ và tự hết hạn: mất khi khởi động lại server thì khách chỉ cần bấm nút trên thẻ.
 */
@Component
public class ChatbotPendingSelections {

    private static final Duration TTL = Duration.ofMinutes(15);

    private record Pending(List<ChatbotMessageResponse.ItemOptionDto> options, Instant expiresAt) {
    }

    private final Map<Integer, Pending> byUser = new ConcurrentHashMap<>();

    public void remember(int userId, List<ChatbotMessageResponse.ItemOptionDto> options) {
        Instant now = Instant.now();
        byUser.values().removeIf(p -> p.expiresAt().isBefore(now));
        byUser.put(userId, new Pending(List.copyOf(options), now.plus(TTL)));
    }

    /** @param position vị trí từ 1, -1 là món cuối. Trả null nếu không có danh sách hoặc vị trí sai. */
    public ChatbotMessageResponse.ItemOptionDto pick(int userId, int position) {
        Pending pending = byUser.get(userId);
        if (pending == null || pending.expiresAt().isBefore(Instant.now())) return null;
        List<ChatbotMessageResponse.ItemOptionDto> options = pending.options();
        int index = position == -1 ? options.size() - 1 : position - 1;
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }

    public boolean has(int userId) {
        Pending pending = byUser.get(userId);
        return pending != null && pending.expiresAt().isAfter(Instant.now());
    }

    public void clear(int userId) {
        byUser.remove(userId);
    }
}
