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
import com.sang.be_capnong.service.CloudinaryService;
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
    private final CloudinaryService cloudinaryService;

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

        String msgType = request.getType() != null && !request.getType().isBlank() ? request.getType() : "TEXT";
        if (request.getAttachmentUrl() != null && !request.getAttachmentUrl().isBlank()) {
            if ("TEXT".equalsIgnoreCase(msgType)) {
                String attach = request.getAttachmentUrl().toLowerCase();
                boolean isVideo = attach.endsWith(".mp4") || attach.endsWith(".mov") || attach.endsWith(".webm") || attach.endsWith(".avi") || attach.endsWith(".mkv");
                msgType = isVideo ? "VIDEO" : "IMAGE";
            }
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(hasContent ? request.getContent().trim() : "")
                .type(msgType)
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

        String notifContent = message.getContent();
        if (notifContent == null || notifContent.isBlank()) {
            notifContent = "VIDEO".equalsIgnoreCase(message.getType()) ? "[Video]" : "[Hình ảnh]";
        }

        Map<String, Object> notificationPayload = Map.of(
                "type", "NEW_MESSAGE",
                "conversationId", conversation.getId(),
                "senderId", senderId,
                "senderName", senderDisplayName,
                "content", notifContent,
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
    @Transactional
    public List<ConversationResponse> getUserConversations(int userId) {
        if (!userRepository.existsById(userId)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        List<Conversation> conversations = conversationRepository.findAllByUserIdOrderByLastMessageAtDesc(userId);
        
        // Loại bỏ trùng lặp theo otherUserId và hợp nhất tin nhắn nếu có nhiều hội thoại của cùng một người dùng
        Map<Integer, Conversation> primaryConvMap = new java.util.LinkedHashMap<>();
        for (Conversation conv : conversations) {
            int otherId = conv.getUser1().getId() == userId ? conv.getUser2().getId() : conv.getUser1().getId();
            if (!primaryConvMap.containsKey(otherId)) {
                primaryConvMap.put(otherId, conv);
            } else {
                Conversation mainConv = primaryConvMap.get(otherId);
                chatMessageRepository.reassignMessagesToConversation(conv.getId(), mainConv);
                conversationRepository.delete(conv);
            }
        }

        Map<Integer, ConversationResponse> uniqueMap = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer, Conversation> entry : primaryConvMap.entrySet()) {
            uniqueMap.put(entry.getKey(), mapToConversationResponse(entry.getValue(), userId));
        }

        User currentUser = userRepository.findById(userId).orElse(null);
        boolean isCurrentUserAdmin = currentUser != null && currentUser.getRole() != null 
                && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        if (isCurrentUserAdmin) {
            // Đối với Admin: Chỉ hiển thị các hội thoại đã có tin nhắn thực tế từ người dùng/nhà vườn
            return uniqueMap.values().stream()
                    .filter(c -> (c.getLastMessage() != null && !c.getLastMessage().isBlank()) || c.getUnreadCount() > 0)
                    .collect(java.util.stream.Collectors.toList());
        }

        // Đối với Shop và Người mua (Buyer): Luôn đảm bảo có sẵn liên hệ Ban Quản Trị (Admin)
        boolean hasAdmin = uniqueMap.values().stream()
                .anyMatch(c -> "ADMIN".equalsIgnoreCase(c.getOtherUserRole()));

        if (!hasAdmin && currentUser != null) {
            Page<User> adminPage = userRepository.findByRole_NameInAndStatus(
                    List.of("ADMIN"), UserStatus.ACTIVE, PageRequest.of(0, 1)
            );
            User admin = !adminPage.isEmpty() ? adminPage.getContent().get(0) : null;
            if (admin == null) {
                Page<User> anyAdminPage = userRepository.findByRole_NameIn(
                        List.of("ADMIN"), PageRequest.of(0, 1)
                );
                if (!anyAdminPage.isEmpty()) {
                    admin = anyAdminPage.getContent().get(0);
                }
            }

            if (admin != null && admin.getId() != userId) {
                Conversation adminConv = findOrCreateConversationEntity(userId, admin.getId(), currentUser, admin);
                ConversationResponse adminResp = mapToConversationResponse(adminConv, userId);
                if (adminResp.getLastMessage() == null || adminResp.getLastMessage().isBlank()) {
                    adminResp.setLastMessage("Liên hệ Ban Quản Trị CapNong để được hỗ trợ 24/7");
                }
                Map<Integer, ConversationResponse> finalMap = new java.util.LinkedHashMap<>();
                finalMap.put(adminResp.getOtherUserId(), adminResp);
                finalMap.putAll(uniqueMap);
                return new java.util.ArrayList<>(finalMap.values());
            }
        }

        return new java.util.ArrayList<>(uniqueMap.values());
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

        // Fallback: Kiểm tra cặp user đã từng có hội thoại nào trước đó chưa để tránh tạo trùng
        List<Conversation> pairList = conversationRepository.findByUserPair(minId, maxId);
        if (!pairList.isEmpty()) {
            Conversation conv = pairList.get(0);
            conv.setRoomKey(roomKey);
            if (pairList.size() > 1) {
                for (int i = 1; i < pairList.size(); i++) {
                    Conversation dup = pairList.get(i);
                    chatMessageRepository.reassignMessagesToConversation(dup.getId(), conv);
                    conversationRepository.delete(dup);
                }
            }
            return conversationRepository.save(conv);
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
        if ("ADMIN".equalsIgnoreCase(role)) {
            otherName = "Ban Quản Trị CapNong";
            if (avatar == null || avatar.isBlank()) {
                avatar = "https://res.cloudinary.com/dfp44sbsj/image/upload/v1774252968/brands/vxm3b2gvixlqzf7xocoy.jpg";
            }
        }

        String lastMsgContent = "";
        if (lastMsg != null) {
            if (lastMsg.getContent() != null && !lastMsg.getContent().isBlank()) {
                lastMsgContent = lastMsg.getContent();
            } else if (lastMsg.getAttachmentUrl() != null && !lastMsg.getAttachmentUrl().isBlank()) {
                String attach = lastMsg.getAttachmentUrl().toLowerCase();
                boolean isVideo = "VIDEO".equalsIgnoreCase(lastMsg.getType())
                        || attach.endsWith(".mp4") || attach.endsWith(".mov") || attach.endsWith(".webm") || attach.endsWith(".avi") || attach.endsWith(".mkv");
                lastMsgContent = isVideo ? "[Video]" : "[Hình ảnh]";
            }
        }

        return ConversationResponse.builder()
                .id(conv.getId())
                .roomKey(conv.getRoomKey())
                .otherUserId(otherUser.getId())
                .otherUserName(otherName)
                .otherUserAvatar(avatar)
                .otherUserRole(role)
                .lastMessage(lastMsgContent)
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

    @Override
    @Transactional
    public void deleteMessage(int messageId, int userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());

        // Chỉ cho phép người gửi tin nhắn HOẶC Admin xóa tin nhắn
        if (message.getSender().getId() != userId && !isAdmin) {
            throw new AppException(ErrorCode.NOT_MESSAGE_SENDER);
        }

        Conversation conversation = message.getConversation();
        int convId = conversation.getId();
        int senderId = message.getSender().getId();
        int receiverId = conversation.getUser1().getId() == senderId
                ? conversation.getUser2().getId()
                : conversation.getUser1().getId();

        boolean wasUnread = Boolean.FALSE.equals(message.getIsRead());
        String attachmentUrl = message.getAttachmentUrl();

        // 1. Dọn dẹp tệp đính kèm trên Cloudinary nếu có (ảnh hoặc video)
        if (attachmentUrl != null && !attachmentUrl.isBlank()) {
            try {
                cloudinaryService.deleteImageByUrl(attachmentUrl);
            } catch (Exception e) {
                log.warn("Failed to delete attachment from Cloudinary for message {}: {}", messageId, e.getMessage());
            }
        }

        // 2. Xóa tin nhắn khỏi cơ sở dữ liệu
        chatMessageRepository.delete(message);
        chatMessageRepository.flush();

        // 3. Cập nhật lại lastMessageAt và preview của conversation theo tin nhắn mới nhất còn lại
        ChatMessage newLastMsg = chatMessageRepository.findFirstByConversation_IdOrderBySentAtDesc(convId)
                .orElse(null);

        String newLastContent = "";
        LocalDateTime newLastTime = null;

        if (newLastMsg != null) {
            conversation.setLastMessageAt(newLastMsg.getSentAt());
            if (newLastMsg.getContent() != null && !newLastMsg.getContent().isBlank()) {
                newLastContent = newLastMsg.getContent();
            } else if (newLastMsg.getAttachmentUrl() != null) {
                boolean isVideo = "VIDEO".equalsIgnoreCase(newLastMsg.getType()) ||
                        newLastMsg.getAttachmentUrl().toLowerCase().matches(".*\\.(mp4|mov|webm|avi|mkv)(\\?.*)?$");
                newLastContent = isVideo ? "[Video]" : "[Hình ảnh]";
            }
            newLastTime = newLastMsg.getSentAt();
        } else {
            conversation.setLastMessageAt(conversation.getCreatedAt());
            newLastTime = conversation.getCreatedAt();
        }
        conversationRepository.save(conversation);

        // 4. Nếu tin nhắn bị xóa chưa được đọc, gửi cập nhật lại unreadCount cho người nhận
        if (wasUnread) {
            long newUnreadTotal = chatMessageRepository.countTotalUnreadForUser(receiverId);
            long convUnread = chatMessageRepository.countByConversation_IdAndSender_IdNotAndIsReadFalse(convId, receiverId);
            try {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(receiverId),
                        "/queue/notifications",
                        Map.of(
                                "type", "UNREAD_COUNT",
                                "conversationId", convId,
                                "conversationUnread", convUnread,
                                "unreadCount", newUnreadTotal
                        )
                );
            } catch (Exception e) {
                log.warn("[WS Chat] Error broadcasting unread update to user {}: {}", receiverId, e.getMessage());
            }
        }

        // 5. Phát tín hiệu thông báo xóa tin nhắn kèm newLastMessage tới cả 2 người qua WebSocket
        Map<String, Object> deleteNotif = new java.util.HashMap<>();
        deleteNotif.put("type", "DELETE_MESSAGE");
        deleteNotif.put("messageId", messageId);
        deleteNotif.put("conversationId", convId);
        deleteNotif.put("newLastMessage", newLastContent);
        if (newLastTime != null) {
            deleteNotif.put("newLastMessageTime", newLastTime.toString());
        }

        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(receiverId),
                    "/queue/notifications",
                    deleteNotif
            );
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notifications",
                    deleteNotif
            );
        } catch (Exception e) {
            log.warn("[WS Chat] Error broadcasting delete notification: {}", e.getMessage());
        }
    }
}
