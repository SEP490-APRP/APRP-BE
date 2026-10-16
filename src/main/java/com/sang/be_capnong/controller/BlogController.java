package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BlogCommentRequest;
import com.sang.be_capnong.dto.request.BlogCreateRequest;
import com.sang.be_capnong.dto.request.BlogUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BlogCommentResponse;
import com.sang.be_capnong.dto.response.BlogResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import com.sang.be_capnong.service.BlogService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogController {

    BlogService blogService;

    // ==========================================
    // 1. PUBLIC ENDPOINTS (Khách & Người dùng)
    // ==========================================

    // Lấy danh sách bài viết (Bảng tin công khai), có lọc category & tìm kiếm từ khóa
    @GetMapping({"/v1/public/blogs", "/blogs", "/v1/blogs"})
    public ApiResponse<PageResponse<BlogResponse>> getPublicBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BlogCategory category,
            @RequestParam(required = false) String search) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getPublicBlogs(page, size, category, search))
                .build();
    }

    // Xem chi tiết bài viết (tự động tăng view)
    @GetMapping({"/v1/public/blogs/{blogId}", "/blogs/{blogId}", "/v1/blogs/{blogId}"})
    public ApiResponse<BlogResponse> getBlogDetail(@PathVariable int blogId) {
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.getPublicBlogDetail(blogId))
                .build();
    }

    // Thả tim / Bỏ tim bài viết (yêu cầu đăng nhập)
    @PostMapping({"/v1/public/blogs/{blogId}/like", "/blogs/{blogId}/like", "/v1/blogs/{blogId}/like"})
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> toggleLike(@PathVariable int blogId) {
        return blogService.toggleLike(blogId);
    }

    // Gửi bình luận bài viết (yêu cầu đăng nhập)
    @PostMapping({"/v1/public/blogs/{blogId}/comments", "/blogs/{blogId}/comments", "/v1/blogs/{blogId}/comments"})
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BlogCommentResponse> addComment(@PathVariable int blogId,
                                                       @Valid @RequestBody BlogCommentRequest request) {
        return ApiResponse.<BlogCommentResponse>builder()
                .result(blogService.addComment(blogId, request))
                .build();
    }

    // Lấy danh sách bình luận của bài viết
    @GetMapping({"/v1/public/blogs/{blogId}/comments", "/blogs/{blogId}/comments", "/v1/blogs/{blogId}/comments"})
    public ApiResponse<PageResponse<BlogCommentResponse>> getComments(
            @PathVariable int blogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BlogCommentResponse>>builder()
                .result(blogService.getComments(blogId, page, size))
                .build();
    }

    // Xóa bình luận (chủ comment, chủ shop bài viết hoặc Admin)
    @DeleteMapping({"/v1/public/blogs/comments/{commentId}", "/blogs/comments/{commentId}", "/v1/blogs/comments/{commentId}"})
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteComment(@PathVariable int commentId) {
        return blogService.deleteComment(commentId);
    }

    // ==========================================
    // 2. SHOP & AUTHOR ENDPOINTS (Shop / Nông dân)
    // ==========================================

    // Lấy danh sách bài viết của shop mình (Shop hoặc Admin)
    @GetMapping({"/v1/shop/blogs", "/blogs/my-blogs", "/v1/blogs/my-blogs"})
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<PageResponse<BlogResponse>> getMyBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getMyBlogs(page, size))
                .build();
    }

    // Tạo bài viết mới (Shop hoặc Admin)
    @PostMapping({"/v1/shop/blogs", "/blogs", "/v1/blogs"})
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<BlogResponse> createBlog(@Valid @RequestBody BlogCreateRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.createBlog(request))
                .build();
    }

    // Chỉnh sửa bài viết (Chính chủ bài viết hoặc Admin)
    @PutMapping({"/v1/shop/blogs/{blogId}", "/blogs/{blogId}", "/v1/blogs/{blogId}"})
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<BlogResponse> updateBlog(@PathVariable int blogId,
                                                @RequestBody BlogUpdateRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.updateBlog(blogId, request))
                .build();
    }

    // Xóa bài viết (Chính chủ bài viết hoặc Admin)
    @DeleteMapping({"/v1/shop/blogs/{blogId}", "/blogs/{blogId}", "/v1/blogs/{blogId}"})
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<Void> deleteBlog(@PathVariable int blogId) {
        return blogService.deleteBlog(blogId);
    }

    // ==========================================
    // 3. ADMIN MANAGEMENT ENDPOINTS
    // ==========================================

    // Admin xem tất cả bài viết toàn sàn (mọi trạng thái: DRAFT, PUBLISHED, HIDDEN)
    @GetMapping({"/v1/admin/blogs", "/blogs/admin/all", "/v1/blogs/admin/all"})
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<BlogResponse>> getAllBlogsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BlogStatus status,
            @RequestParam(required = false) BlogCategory category,
            @RequestParam(required = false) String search) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getAllBlogsForAdmin(page, size, status, category, search))
                .build();
    }

    // Đổi trạng thái bài viết (Chính chủ Shop hoặc Admin kiểm duyệt)
    @PatchMapping({"/v1/admin/blogs/{blogId}/status", "/v1/shop/blogs/{blogId}/status", "/blogs/{blogId}/status", "/v1/blogs/{blogId}/status"})
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<BlogResponse> updateBlogStatus(@PathVariable int blogId,
                                                      @RequestParam BlogStatus status) {
        BlogUpdateRequest request = BlogUpdateRequest.builder()
                .status(status)
                .build();
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.updateBlog(blogId, request))
                .build();
    }
}
