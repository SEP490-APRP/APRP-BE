package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ChatMessageRequest;
import com.sang.be_capnong.dto.response.ChatMessageResponse;
import com.sang.be_capnong.dto.response.ConversationResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.entity.ChatMessage;
import com.sang.be_capnong.entity.Conversation;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.ChatMessageRepository;
import com.sang.be_capnong.repository.ConversationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ConversationResponse getOrCreateConversation(int currentUserId, int receiverId) {
        if (currentUserId == receiverId) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new AppException(ErrorCode.RECEIVER_NOT_FOUND));

        validateUserActive(currentUser);
        validateUserActive(receiver);

        Conversation conversation = findOrCreateConversationEntity(currentUserId, receiverId, currentUser, receiver);
        return mapToConversationResponse(conversation, currentUserId);
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(int senderId, ChatMessageRequest request) {
        if (request.getReceiverId() == null) {
            throw new AppException(ErrorCode.RECEIVER_NOT_FOUND);
        }
        if (senderId == request.getReceiverId()) {
            throw new AppException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasAttachment = request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank();
        if (!hasContent && !hasAttachment) {
            throw new AppException(ErrorCode.EMPTY_MESSAGE_CONTENT);
        }

        if (hasAttachment) {
            String url = request.getAttachmentUrl().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw new AppException(ErrorCode.FILE_INVALID);
            }
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.RECEIVER_NOT_FOUND));

        validateUserActive(sender);
        validateUserActive(receiver);

        Conversation conversation = findOrCreateConversationEntity(senderId, request.getReceiverId(), sender, receiver);

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(hasContent ? request.getContent().trim() : "")
                .type(request.getType() != null && !request.getType().isBlank() ? request.getType() : "TEXT")
                .attachmentUrl(request.getAttachmentUrl())
                .referenceId(request.getReferenceId())
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        message = chatMessageRepository.save(message);

        conversation.setLastMessageAt(message.getSentAt());
        conversationRepository.save(conversation);

        ChatMessageResponse response = mapToChatMessageResponse(message, request.getReceiverId());

        long unreadCount = chatMessageRepository.countTotalUnreadForUser(request.getReceiverId());
        String senderDisplayName = sender.getFullName() != null && !sender.getFullName().isBlank()
                ? sender.getFullName()
                : (sender.getEmail() != null ? sender.getEmail() : "Người dùng");

        Map<String, Object> notificationPayload = Map.of(
                "type", "NEW_MESSAGE",
                "conversationId", conversation.getId(),
                "senderId", senderId,
                "senderName", senderDisplayName,
                "content", message.getContent(),
                "unreadCount", unreadCount
        );

        Runnable sendWebSockets = () -> {
            try {
                // 1. Gửi tin nhắn real-time tới người nhận qua private queue an toàn
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(request.getReceiverId()),
                        "/queue/messages",
                        response
                );

                // 2. Gửi echo tin nhắn real-time về cho chính người gửi để đồng bộ đa thiết bị/tab
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(senderId),
                        "/queue/messages",
                        response
                );

                // 3. Bắn push notification cho người nhận kèm tổng số tin chưa đọc
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(request.getReceiverId()),
                        "/queue/notifications",
                        notificationPayload
                );
            } catch (Exception e) {
                log.error("Failed to deliver WebSocket message for conv {}: {}", conversation.getId(), e.getMessage());
            }
        };

        // Chỉ phát WebSocket SAU KHI transaction DB commit thành công để tránh Phantom Messages
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendWebSockets.run();
                        }
                    }
            );
        } else {
            sendWebSockets.run();
        }

        log.info("Message sent from {} to {} in conversation {}", senderId, request.getReceiverId(), conversation.getId());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getUserConversations(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        List<Conversation> conversations = conversationRepository.findAllByUserIdOrderByLastMessageAtDesc(userId);
        return conversations.stream()
                .map(conv -> mapToConversationResponse(conv, userId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMessages(int conversationId, int userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getUser1().getId() != userId && conversation.getUser2().getId() != userId) {
            throw new AppException(ErrorCode.CHAT_UNAUTHORIZED);
        }

        int otherUserId = conversation.getUser1().getId() == userId
                ? conversation.getUser2().getId()
                : conversation.getUser1().getId();

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Order.desc("sentAt"), Sort.Order.desc("id"))
        );
        Page<ChatMessage> messagePage = chatMessageRepository.findByConversation_IdOrderBySentAtDesc(conversationId, pageable);

        Page<ChatMessageResponse> responsePage = messagePage.map(m -> mapToChatMessageResponse(m, otherUserId));
        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional
    public void markAsRead(int conversationId, int userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (conversation.getUser1().getId() != userId && conversation.getUser2().getId() != userId) {
            throw new AppException(ErrorCode.CHAT_UNAUTHORIZED);
        }

        int updated = chatMessageRepository.markAllAsReadInConversation(conversationId, userId);
        log.info("Marked {} messages as read in conv {} by user {}", updated, conversationId, userId);

        int otherUserId = conversation.getUser1().getId() == userId
                ? conversation.getUser2().getId()
                : conversation.getUser1().getId();

        Runnable sendReadReceipt = () -> {
            try {
                // Gửi thông báo read receipt tới người gửi đối phương
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(otherUserId),
                        "/queue/read",
                        Map.of("conversationId", conversationId, "readBy", userId)
                );
            } catch (Exception e) {
                log.error("Failed to deliver read receipt for conv {}: {}", conversationId, e.getMessage());
            }
        };

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendReadReceipt.run();
                        }
                    }
            );
        } else {
            sendReadReceipt.run();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalUnreadCount(int userId) {
        return chatMessageRepository.countTotalUnreadForUser(userId);
    }

    private void validateUserActive(User user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new AppException(ErrorCode.USER_PENDING_APPROVAL);
        }
    }

    /**
     * Tìm hoặc tạo Conversation với cơ chế an toàn chống Race Condition
     */
    private Conversation findOrCreateConversationEntity(int userAId, int userBId, User userA, User userB) {
        int minId = Math.min(userAId, userBId);
        int maxId = Math.max(userAId, userBId);
        String roomKey = minId + "_" + maxId;

        Optional<Conversation> existing = conversationRepository.findByRoomKey(roomKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            log.info("Creating new conversation roomKey: {}", roomKey);
            return conversationRepository.save(
                    Conversation.builder()
                            .roomKey(roomKey)
                            .user1(minId == userAId ? userA : userB)
                            .user2(maxId == userAId ? userA : userB)
                            .createdAt(LocalDateTime.now())
                            .lastMessageAt(LocalDateTime.now())
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent conversation creation detected for roomKey: {}. Falling back to fetch.", roomKey);
            return conversationRepository.findByRoomKey(roomKey)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        }
    }

    private ConversationResponse mapToConversationResponse(Conversation conv, int currentUserId) {
        User otherUser = conv.getUser1().getId() == currentUserId ? conv.getUser2() : conv.getUser1();

        ChatMessage lastMsg = chatMessageRepository.findFirstByConversation_IdOrderBySentAtDesc(conv.getId())
                .orElse(null);

        long unreadCount = chatMessageRepository.countByConversation_IdAndSender_IdNotAndIsReadFalse(conv.getId(), currentUserId);

        String otherName = otherUser.getFullName() != null && !otherUser.getFullName().isBlank()
                ? otherUser.getFullName()
                : (otherUser.getEmail() != null ? otherUser.getEmail() : "Người dùng");

        String avatar = otherUser.getLogoUrl() != null && !otherUser.getLogoUrl().isBlank()
                ? otherUser.getLogoUrl()
                : "";

        String role = otherUser.getRole() != null ? otherUser.getRole().getName() : "BUYER";

        return ConversationResponse.builder()
                .id(conv.getId())
                .roomKey(conv.getRoomKey())
                .otherUserId(otherUser.getId())
                .otherUserName(otherName)
                .otherUserAvatar(avatar)
                .otherUserRole(role)
                .lastMessage(lastMsg != null ? lastMsg.getContent() : "")
                .lastMessageTime(lastMsg != null ? lastMsg.getSentAt() : conv.getLastMessageAt())
                .unreadCount(unreadCount)
                .build();
    }

    private ChatMessageResponse mapToChatMessageResponse(ChatMessage msg, int receiverId) {
        User sender = msg.getSender();
        String senderName = sender.getFullName() != null && !sender.getFullName().isBlank()
                ? sender.getFullName()
                : (sender.getEmail() != null ? sender.getEmail() : "Người dùng");

        return ChatMessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversation().getId())
                .senderId(sender.getId())
                .senderName(senderName)
                .senderAvatar(sender.getLogoUrl() != null ? sender.getLogoUrl() : "")
                .receiverId(receiverId)
                .content(msg.getContent())
                .type(msg.getType() != null ? msg.getType() : "TEXT")
                .attachmentUrl(msg.getAttachmentUrl())
                .referenceId(msg.getReferenceId())
                .isRead(Boolean.TRUE.equals(msg.getIsRead()))
                .sentAt(msg.getSentAt())
                .build();
    }
}
