package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.SavedAddressRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.SavedAddressResponse;
import com.sang.be_capnong.service.SavedAddressService;
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
@RequestMapping("/addresses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Saved Address Book", description = "APIs quản lý sổ địa chỉ nhận hàng của người dùng (tối đa 10 địa chỉ)")
public class SavedAddressController {

    SavedAddressService savedAddressService;

    @Operation(summary = "Lấy danh sách địa chỉ đã lưu", description = "Lấy toàn bộ sổ địa chỉ nhận hàng đang hoạt động của người dùng (ưu tiên địa chỉ mặc định lên đầu).")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<SavedAddressResponse>> getMyAddresses() {
        return ApiResponse.<List<SavedAddressResponse>>builder()
                .result(savedAddressService.getMyAddresses())
                .message("Lấy danh sách địa chỉ thành công.")
                .build();
    }

    @Operation(summary = "Lấy chi tiết một địa chỉ theo ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SavedAddressResponse> getAddressById(@PathVariable("id") Long id) {
        return ApiResponse.<SavedAddressResponse>builder()
                .result(savedAddressService.getAddressById(id))
                .message("Lấy thông tin địa chỉ thành công.")
                .build();
    }

    @Operation(summary = "Lấy địa chỉ mặc định của người dùng")
    @GetMapping("/default")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SavedAddressResponse> getDefaultAddress() {
        return ApiResponse.<SavedAddressResponse>builder()
                .result(savedAddressService.getDefaultAddress())
                .message("Lấy địa chỉ mặc định thành công.")
                .build();
    }

    @Operation(summary = "Thêm địa chỉ nhận hàng mới", description = "Tạo mới một địa chỉ nhận hàng (tối đa 10 địa chỉ/người dùng).")
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SavedAddressResponse> createAddress(@RequestBody @Valid SavedAddressRequest request) {
        return ApiResponse.<SavedAddressResponse>builder()
                .result(savedAddressService.createAddress(request))
                .message("Thêm địa chỉ mới thành công.")
                .build();
    }

    @Operation(summary = "Cập nhật địa chỉ nhận hàng đã lưu", description = "Sửa thông tin của một địa chỉ cụ thể theo ID (UPDATE thay vì tạo mới).")
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SavedAddressResponse> updateAddress(
            @PathVariable("id") Long id,
            @RequestBody @Valid SavedAddressRequest request
    ) {
        return ApiResponse.<SavedAddressResponse>builder()
                .result(savedAddressService.updateAddress(id, request))
                .message("Cập nhật địa chỉ thành công.")
                .build();
    }

    @Operation(summary = "Xóa địa chỉ nhận hàng")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteAddress(@PathVariable("id") Long id) {
        savedAddressService.deleteAddress(id);
        return ApiResponse.<Void>builder()
                .message("Xóa địa chỉ thành công.")
                .build();
    }

    @Operation(summary = "Thiết lập địa chỉ mặc định")
    @PatchMapping("/{id}/default")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<SavedAddressResponse> setDefaultAddress(@PathVariable("id") Long id) {
        return ApiResponse.<SavedAddressResponse>builder()
                .result(savedAddressService.setDefaultAddress(id))
                .message("Đã đặt làm địa chỉ mặc định.")
                .build();
    }
}
