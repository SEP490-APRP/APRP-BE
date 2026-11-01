package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.NotificationRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.NotificationResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/notifications", "/api/v1/notifications", "/api/notifications"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notification APIs", description = "Quản lý và thông báo đẩy người dùng")
public class NotificationController {

    NotificationService notificationService;

    // =====================================================================
    // ADMIN: GỬI THÔNG BÁO CHO CÁC NHÓM
    // =====================================================================
    @PostMapping("/admin/send-to-groups")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin gửi thông báo", description = "Gửi thông báo tới các nhóm đối tượng (BUYER, SHOP, SHIPPER) hoặc tất cả.")
    public ApiResponse<Void> adminSendToGroups(@Valid @RequestBody NotificationRequest request) {
        notificationService.createNotificationForGroups(request);
        return ApiResponse.<Void>builder()
                .message("Gửi thông báo thành công đến các nhóm: " + String.join(", ", request.getReceiverTypes()))
                .build();
    }

    // =====================================================================
    // XEM THÔNG BÁO CÁ NHÂN & TOÀN HỆ THỐNG
    // =====================================================================
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem thông báo cá nhân", description = "Lấy danh sách thông báo của người dùng đang đăng nhập.")
    public ApiResponse<List<NotificationResponse>> getMyNotifications() {
        List<NotificationResponse> notifications = notificationService.getMyNotifications();
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notifications)
                .message("Lấy danh sách thông báo thành công.")
                .build();
    }

    @GetMapping("/my/paged")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem thông báo cá nhân có phân trang", description = "Lấy danh sách thông báo phân trang của người dùng đang đăng nhập.")
    public ApiResponse<PageResponse<NotificationResponse>> getMyNotificationsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<NotificationResponse> pageResponse = notificationService.getMyNotificationsPaged(page, size);
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .result(pageResponse)
                .message("Lấy danh sách thông báo phân trang thành công.")
                .build();
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đếm số thông báo chưa đọc", description = "Lấy số lượng thông báo chưa đọc của người dùng hiện tại.")
    public ApiResponse<Long> getMyUnreadCount() {
        long count = notificationService.getMyUnreadCount();
        return ApiResponse.<Long>builder()
                .result(count)
                .message("Lấy số lượng thông báo chưa đọc thành công.")
                .build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin xem toàn bộ lịch sử thông báo", description = "Lấy toàn bộ thông báo hệ thống đã phát hành.")
    public ApiResponse<List<NotificationResponse>> getAllNotifications() {
        List<NotificationResponse> notifications = notificationService.getAllNotifications();
        return ApiResponse.<List<NotificationResponse>>builder()
                .result(notifications)
                .message("Lấy tất cả thông báo thành công.")
                .build();
    }

    @GetMapping({"/all/paged", "/paged"})
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin xem toàn bộ lịch sử thông báo có phân trang và lọc", description = "Lấy danh sách thông báo phân trang của toàn hệ thống, hỗ trợ lọc theo nhóm đối tượng và tìm kiếm từ khóa.")
    public ApiResponse<PageResponse<NotificationResponse>> getAllNotificationsPaged(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        PageResponse<NotificationResponse> pageResponse = notificationService.getAllNotificationsPaged(page, size, role, keyword);
        return ApiResponse.<PageResponse<NotificationResponse>>builder()
                .result(pageResponse)
                .message("Lấy danh sách thông báo phân trang thành công.")
                .build();
    }

    // =====================================================================
    // THAO TÁC TRÊN THÔNG BÁO
    // =====================================================================
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu 1 thông báo cụ thể là đã đọc.")
    public ApiResponse<Void> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu đọc thông báo.")
                .build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đánh dấu tất cả đã đọc", description = "Đánh dấu toàn bộ thông báo của người dùng là đã đọc.")
    public ApiResponse<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ApiResponse.<Void>builder()
                .message("Đã đánh dấu tất cả thông báo là đã đọc.")
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xoá thông báo", description = "Xoá mềm thông báo khỏi hộp thư người dùng.")
    public ApiResponse<Void> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotification(id);
        return ApiResponse.<Void>builder()
                .message("Đã xóa thông báo.")
                .build();
    }
}
