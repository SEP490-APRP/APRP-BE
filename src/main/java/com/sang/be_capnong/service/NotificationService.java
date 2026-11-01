package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.NotificationRequest;
import com.sang.be_capnong.dto.response.NotificationResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.entity.Notification;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.UserNotification;

import java.util.List;

public interface NotificationService {

    void createNotificationForGroups(NotificationRequest request);

    List<NotificationResponse> getMyNotifications();

    PageResponse<NotificationResponse> getMyNotificationsPaged(int page, int size);

    List<NotificationResponse> getAllNotifications();

    PageResponse<NotificationResponse> getAllNotificationsPaged(int page, int size);

    PageResponse<NotificationResponse> getAllNotificationsPaged(int page, int size, String role, String keyword);

    long getMyUnreadCount();

    void markAsRead(Integer userNotificationId);

    void markAllAsRead();

    void deleteNotification(Integer userNotificationId);

    UserNotification createUserNotification(User user, Notification notification);

    void sendNotificationToUser(User user, String title, String message, String evidence);

    void sendNotificationWebsocket(User user, Notification notification);
}
