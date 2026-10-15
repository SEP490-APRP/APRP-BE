package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.BlogCommentRequest;
import com.sang.be_capnong.dto.request.BlogCreateRequest;
import com.sang.be_capnong.dto.request.BlogUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BlogCommentResponse;
import com.sang.be_capnong.dto.response.BlogResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;

public interface BlogService {

    // --- DÀNH CHO SHOP & ADMIN ---
    BlogResponse createBlog(BlogCreateRequest request);
    BlogResponse updateBlog(int blogId, BlogUpdateRequest request);
    ApiResponse<Void> deleteBlog(int blogId);
    PageResponse<BlogResponse> getMyBlogs(int page, int size);

    // --- DÀNH CHO PUBLIC ---
    PageResponse<BlogResponse> getPublicBlogs(int page, int size, BlogCategory category, String search);
    default PageResponse<BlogResponse> getPublicBlogs(int page, int size, BlogCategory category) {
        return getPublicBlogs(page, size, category, null);
    }
    BlogResponse getPublicBlogDetail(int blogId);

    // --- DÀNH CHO ADMIN ---
    PageResponse<BlogResponse> getAllBlogsForAdmin(int page, int size, BlogStatus status, BlogCategory category, String search);

    // --- TƯƠNG TÁC (LIKE & COMMENT) ---
    ApiResponse<String> toggleLike(int blogId);
    BlogCommentResponse addComment(int blogId, BlogCommentRequest request);
    PageResponse<BlogCommentResponse> getComments(int blogId, int page, int size);
    ApiResponse<Void> deleteComment(int commentId);
}
