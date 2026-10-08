package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BlogCreateRequest;
import com.sang.be_capnong.dto.request.BlogUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BlogResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.service.BlogService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shop/blogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogShopController {

    BlogService blogService;

    @PostMapping
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<BlogResponse> createBlog(@Valid @RequestBody BlogCreateRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.createBlog(request))
                .build();
    }

    @PutMapping("/{blogId}")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<BlogResponse> updateBlog(@PathVariable int blogId,
                                                @RequestBody BlogUpdateRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .result(blogService.updateBlog(blogId, request))
                .build();
    }

    @DeleteMapping("/{blogId}")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<Void> deleteBlog(@PathVariable int blogId) {
        return blogService.deleteBlog(blogId);
    }

    @GetMapping
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<PageResponse<BlogResponse>> getMyBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .result(blogService.getMyBlogs(page, size))
                .build();
    }
}
