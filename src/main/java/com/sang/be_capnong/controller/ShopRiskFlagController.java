package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ShopRiskFlagResponse;
import com.sang.be_capnong.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Penalty & Risk Management", description = "Quản lý vi phạm bom hàng của khách và cờ rủi ro chất lượng của Shop")
public class ShopRiskFlagController {

    UserService userService;

    @Operation(summary = "Shop xem danh sách Risk Flags của mình",
            description = "[SHOP] Xem các cờ rủi ro vi phạm do Admin xác nhận khiếu nại hàng hỏng.")
    @GetMapping("/shops/my-risk-flags")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<List<ShopRiskFlagResponse>> getMyRiskFlags() {
        return ApiResponse.<List<ShopRiskFlagResponse>>builder()
                .result(userService.getMyRiskFlags())
                .message("Lấy danh sách cờ rủi ro thành công")
                .build();
    }

    @Operation(summary = "Admin xem danh sách Risk Flags của một Shop",
            description = "[ADMIN] Xem chi tiết lịch sử cờ rủi ro của shop theo shopId.")
    @GetMapping("/admin/shops/{shopId}/risk-flags")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ShopRiskFlagResponse>> getShopRiskFlags(@PathVariable int shopId) {
        return ApiResponse.<List<ShopRiskFlagResponse>>builder()
                .result(userService.getShopRiskFlags(shopId))
                .message("Lấy danh sách cờ rủi ro của shop thành công")
                .build();
    }

    @Operation(summary = "Admin gỡ bỏ / giải quyết một Risk Flag",
            description = "[ADMIN] Gỡ bỏ 1 cờ rủi ro. Nếu số cờ active giảm dưới 3, hệ thống tự động hoàn lại 1 sao đánh giá và khôi phục hiển thị sản phẩm.")
    @PutMapping("/admin/risk-flags/{flagId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resolveRiskFlag(
            @PathVariable int flagId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = (body != null) ? body.get("note") : null;
        userService.resolveRiskFlag(flagId, note);
        return ApiResponse.<Void>builder()
                .message("Đã giải quyết cờ rủi ro thành công")
                .build();
    }

    @Operation(summary = "Admin xóa vi phạm bom hàng cho khách hàng",
            description = "[ADMIN] Khôi phục quyền thanh toán COD và mở khóa tài khoản cho khách khi có khiếu nại xác đáng.")
    @PutMapping("/admin/users/{userId}/reset-customer-penalties")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> resetCustomerPenalties(@PathVariable int userId) {
        userService.resetCustomerPenalties(userId);
        return ApiResponse.<Void>builder()
                .message("Đã khôi phục quyền tài khoản cho khách hàng thành công")
                .build();
    }
}
