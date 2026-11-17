package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.NotificationRequest;
import com.sang.be_capnong.dto.response.NotificationResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.entity.Notification;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.UserNotification;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.NotificationMapper;
import com.sang.be_capnong.repository.NotificationRepository;
import com.sang.be_capnong.repository.UserNotificationRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.repository.specification.NotificationSpecification;
import com.sang.be_capnong.service.ExpoPushService;
import com.sang.be_capnong.service.NotificationService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationServiceImpl implements NotificationService {

    NotificationRepository notificationRepository;
    UserNotificationRepository userNotificationRepository;
    UserRepository userRepository;
    NotificationMapper notificationMapper;
    SimpMessagingTemplate simpMessagingTemplate;
    ExpoPushService expoPushService;

    @Override
    @Transactional
    public void createNotificationForGroups(NotificationRequest request) {
        Integer adminId = SecurityUtils.getCurrentUserId();
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getReceiverTypes() == null || request.getReceiverTypes().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_NOTIFICATION_REQUEST);
        }

        Set<String> canonicalRoles = new HashSet<>();
        for (String r : request.getReceiverTypes()) {
            if (r == null) continue;
            String upper = r.trim().toUpperCase();
            if ("SHOP_OWNER".equals(upper) || "SHOP".equals(upper) || "FARMER".equals(upper) || "SELLER".equals(upper) || "ROLE_SHOP".equals(upper)) {
                canonicalRoles.add("SHOP");
            } else if ("BUYER".equals(upper) || "USER".equals(upper) || "ROLE_BUYER".equals(upper)) {
                canonicalRoles.add("BUYER");
            } else if ("SHIPPER".equals(upper) || "ROLE_SHIPPER".equals(upper)) {
                canonicalRoles.add("SHIPPER");
            }
        }

        if (canonicalRoles.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_NOTIFICATION_REQUEST);
        }

        String receiverType = String.join(",", canonicalRoles);

        Notification notification = Notification.builder()
                .admin(admin)
                .title(request.getTitle())
                .message(request.getMessage())
                .evidence(request.getEvidence())
                .receiverType(receiverType)
                .createAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        List<User> targetUsers = userRepository.findByRole_NameInAndStatus(canonicalRoles, UserStatus.ACTIVE);

        log.info("[Notification] Broadcasting notification '{}' to {} active users with roles: {}",
                savedNotification.getTitle(), targetUsers.size(), canonicalRoles);

        if (targetUsers.isEmpty()) {
            return;
        }

        // Batch create and persist user notifications
        List<UserNotification> userNotifications = new ArrayList<>(targetUsers.size());
        for (User u : targetUsers) {
            userNotifications.add(UserNotification.builder()
                    .user(u)
                    .notification(savedNotification)
                    .isRead(false)
                    .isDeleted(false)
                    .build());
        }

        List<UserNotification> savedUserNotifications = userNotificationRepository.saveAll(userNotifications);

        // Realtime dispatch
        for (UserNotification un : savedUserNotifications) {
            NotificationResponse response = notificationMapper.toResponse(un.getNotification());
            response.setId(un.getId());
            response.setIsRead(un.getIsRead());

            try {
                // 1. Direct queue for user
                simpMessagingTemplate.convertAndSend(
                        "/queue/notifications." + un.getUser().getId(),
                        response
                );
                // 2. Private STOMP user queue
                simpMessagingTemplate.convertAndSendToUser(
                        String.valueOf(un.getUser().getId()),
                        "/queue/notifications",
                        response
                );
            } catch (Exception e) {
                log.warn("[Notification] Could not deliver WebSocket message to user id={}: {}",
                        un.getUser().getId(), e.getMessage());
            }
        }

        // 3. Batch Push Notification ra điện thoại cho tất cả target users
        try {
            List<Integer> targetUserIds = targetUsers.stream().map(User::getId).collect(Collectors.toList());
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("notiId", savedNotification.getNotiId());
            pushData.put("type", "ADMIN_BROADCAST");
            pushData.put("title", savedNotification.getTitle());
            expoPushService.sendPushToUsersAsync(targetUserIds, savedNotification.getTitle(), savedNotification.getMessage(), pushData, "general_promotions");
        } catch (Exception e) {
            log.warn("[Notification] Could not trigger push notifications for group broadcast: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserNotification createUserNotification(User user, Notification notification) {
        UserNotification userNotification = UserNotification.builder()
                .user(user)
                .notification(notification)
                .isRead(false)
                .isDeleted(false)
                .build();
        return userNotificationRepository.save(userNotification);
    }

    @Override
    public void sendNotificationWebsocket(User user, Notification notification) {
        UserNotification un = createUserNotification(user, notification);
        NotificationResponse response = notificationMapper.toResponse(un.getNotification());
        response.setId(un.getId());
        response.setIsRead(un.getIsRead());

        try {
            // 1. Gửi qua direct public queue /queue/notifications.{userId}
            simpMessagingTemplate.convertAndSend(
                    "/queue/notifications." + user.getId(),
                    response
            );
            // 2. Gửi qua user-specific private queue /user/{userId}/queue/notifications
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(user.getId()),
                    "/queue/notifications",
                    response
            );
            log.info("[Notification] Sent WebSocket to user id={}", user.getId());
        } catch (Exception e) {
            log.warn("Failed to send notification via WebSocket to user {}: {}", user.getId(), e.getMessage());
        }

        // 3. Gửi Push Notification ra điện thoại (hỗ trợ khi app chạy ngầm hoặc tắt hẳn)
        try {
            Map<String, Object> pushData = new HashMap<>();
            pushData.put("notificationId", un.getId());
            pushData.put("notiId", notification.getNotiId());
            pushData.put("type", notification.getType() != null ? notification.getType() : "GENERAL");
            if (notification.getReferenceId() != null) {
                pushData.put("referenceId", notification.getReferenceId());
                pushData.put("orderId", notification.getReferenceId());
            }
            if (user.getRole() != null) {
                pushData.put("role", user.getRole().getName());
            }
            String channelId = resolveChannelId(notification.getType());

            expoPushService.sendPushToUserAsync(
                    user.getId(),
                    notification.getTitle(),
                    notification.getMessage(),
                    pushData,
                    channelId
            );
        } catch (Exception e) {
            log.warn("Failed to trigger push notification for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private String resolveChannelId(String notiType) {
        if (notiType == null) return "general_promotions";
        String upper = notiType.toUpperCase();
        if (upper.startsWith("ORDER_") || upper.startsWith("NEW_ORDER") || upper.startsWith("SHIPPER_") || upper.startsWith("DISPATCH_")) {
            return "order_updates";
        }
        if (upper.startsWith("CHAT_")) {
            return "chat_messages";
        }
        return "general_promotions";
    }

    @Override
    @Transactional
    public void sendNotificationToUser(User user, String title, String message, String evidence) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .evidence(evidence)
                .receiverType(user.getRole() != null ? user.getRole().getName() : "BUYER")
                .createAt(LocalDateTime.now())
                .build();
        Notification savedNotification = notificationRepository.save(notification);
        sendNotificationWebsocket(user, savedNotification);
    }

    @Override
    @Transactional
    public void sendNotificationToUser(User user, String title, String message, String type, Integer referenceId) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .receiverType(user.getRole() != null ? user.getRole().getName() : "BUYER")
                .createAt(LocalDateTime.now())
                .build();
        sendNotificationWebsocket(user, notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<UserNotification> userNotifications = userNotificationRepository
                .findByUserAndIsDeletedFalseOrderByNotificationCreateAtDesc(currentUser);

        return userNotifications.stream()
                .map(un -> {
                    NotificationResponse response = notificationMapper.toResponse(un.getNotification());
                    response.setId(un.getId());
                    response.setIsRead(un.getIsRead());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotificationsPaged(int page, int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<UserNotification> userNotificationPage = userNotificationRepository
                .findByUserAndIsDeletedFalseOrderByNotificationCreateAtDesc(currentUser, pageable);

        List<NotificationResponse> responses = userNotificationPage.getContent().stream()
                .map(un -> {
                    NotificationResponse response = notificationMapper.toResponse(un.getNotification());
                    response.setId(un.getId());
                    response.setIsRead(un.getIsRead());
                    return response;
                })
                .collect(Collectors.toList());

        return PageResponse.from(userNotificationPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public long getMyUnreadCount() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userNotificationRepository.countByUserAndIsDeletedFalseAndIsReadFalse(currentUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications() {
        List<Notification> notifications = notificationRepository.findAllByOrderByCreateAtDesc();
        return notifications.stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllNotificationsPaged(int page, int size) {
        return getAllNotificationsPaged(page, size, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getAllNotificationsPaged(int page, int size, String role, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createAt"));
        Specification<Notification> spec = NotificationSpecification.filterNotifications(role, keyword);

        Page<Notification> notificationPage = notificationRepository.findAll(spec, pageable);

        List<NotificationResponse> responses = notificationPage.getContent().stream()
                .map(notificationMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.from(notificationPage, responses);
    }

    @Override
    @Transactional
    public void markAsRead(Integer userNotificationId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserNotification un = userNotificationRepository.findByIdAndUser(userNotificationId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        un.setIsRead(true);
        userNotificationRepository.save(un);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userNotificationRepository.markAllAsReadByUser(currentUser);
    }

    @Override
    @Transactional
    public void deleteNotification(Integer userNotificationId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        UserNotification un = userNotificationRepository.findByIdAndUser(userNotificationId, currentUser)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        un.setIsDeleted(true);
        userNotificationRepository.save(un);
    }
}
