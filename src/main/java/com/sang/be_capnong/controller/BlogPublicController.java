package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BlogCommentRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BlogCommentResponse;
import com.sang.be_capnong.dto.response.BlogResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.service.BlogService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/blogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogPublicController {

    BlogService blogService;

    // Lấy danh sách bài viết (Bảng tin), có thể lọc theo category
    @GetMapping
    public ApiResponse<PageResponse<BlogResponse>> getPublicBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BlogCategory category) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getPublicBlogs(page, size, category))
                .build();
    }

    // Xem chi tiết bài viết (đồng thời tăng view)
    @GetMapping("/{blogId}")
    public ApiResponse<BlogResponse> getBlogDetail(@PathVariable int blogId) {
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.getPublicBlogDetail(blogId))
                .build();
    }

    // Toggle Like / Unlike (yêu cầu đăng nhập)
    @PostMapping("/{blogId}/like")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> toggleLike(@PathVariable int blogId) {
        return blogService.toggleLike(blogId);
    }

    // Gửi bình luận (yêu cầu đăng nhập)
    @PostMapping("/{blogId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BlogCommentResponse> addComment(@PathVariable int blogId,
                                                       @Valid @RequestBody BlogCommentRequest request) {
        return ApiResponse.<BlogCommentResponse>builder()
                .result(blogService.addComment(blogId, request))
                .build();
    }

    // Lấy danh sách bình luận
    @GetMapping("/{blogId}/comments")
    public ApiResponse<PageResponse<BlogCommentResponse>> getComments(
            @PathVariable int blogId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BlogCommentResponse>>builder()
                .result(blogService.getComments(blogId, page, size))
                .build();
    }

    // Xóa bình luận (chủ comment hoặc chủ shop bài viết)
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteComment(@PathVariable int commentId) {
        return blogService.deleteComment(commentId);
    }
}
