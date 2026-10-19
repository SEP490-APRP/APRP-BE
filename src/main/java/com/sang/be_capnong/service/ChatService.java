package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ChatMessageRequest;
import com.sang.be_capnong.dto.response.ChatMessageResponse;
import com.sang.be_capnong.dto.response.ConversationResponse;
import com.sang.be_capnong.dto.response.PageResponse;

import java.util.List;

public interface ChatService {

    ConversationResponse getOrCreateConversation(int currentUserId, int receiverId);

    ChatMessageResponse sendMessage(int senderId, ChatMessageRequest request);

    List<ConversationResponse> getUserConversations(int userId);

    PageResponse<ChatMessageResponse> getMessages(int conversationId, int userId, int page, int size);

    void markAsRead(int conversationId, int userId);

    long getTotalUnreadCount(int userId);
}
