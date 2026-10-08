package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.BlogCommentRequest;
import com.sang.be_capnong.dto.request.BlogCreateRequest;
import com.sang.be_capnong.dto.request.BlogUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BlogCommentResponse;
import com.sang.be_capnong.dto.response.BlogResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.entity.Blog;
import com.sang.be_capnong.entity.BlogComment;
import com.sang.be_capnong.entity.BlogLike;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.BlogCommentRepository;
import com.sang.be_capnong.repository.BlogLikeRepository;
import com.sang.be_capnong.repository.BlogRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.BlogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogServiceImpl implements BlogService {

    BlogRepository blogRepository;
    BlogLikeRepository blogLikeRepository;
    BlogCommentRepository blogCommentRepository;
    UserRepository userRepository;

    // ===================== SELLER APIs =====================

    @Transactional
    @Override
    public BlogResponse createBlog(BlogCreateRequest request) {
        User shop = getCurrentUser();
        Blog blog = Blog.builder()
                .shop(shop)
                .title(request.getTitle())
                .content(request.getContent())
                .pictureUrl(request.getPictureUrl())
                .status(request.getStatus())
                .category(request.getCategory())
                .createAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .views(0)
                .build();
        return mapToResponse(blogRepository.save(blog), shop, shop);
    }

    @Transactional
    @Override
    public BlogResponse updateBlog(int blogId, BlogUpdateRequest request) {
        User shop = getCurrentUser();
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getShop().getId() != shop.getId()) {
            throw new AppException(ErrorCode.NOT_BLOG_OWNER);
        }

        if (request.getTitle() != null) blog.setTitle(request.getTitle());
        if (request.getContent() != null) blog.setContent(request.getContent());
        if (request.getPictureUrl() != null) blog.setPictureUrl(request.getPictureUrl());
        if (request.getStatus() != null) blog.setStatus(request.getStatus());
        if (request.getCategory() != null) blog.setCategory(request.getCategory());
        blog.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(blogRepository.save(blog), shop, shop);
    }

    @Transactional
    @Override
    public ApiResponse<Void> deleteBlog(int blogId) {
        User shop = getCurrentUser();
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getShop().getId() != shop.getId()) {
            throw new AppException(ErrorCode.NOT_BLOG_OWNER);
        }

        blogRepository.delete(blog);
        return ApiResponse.<Void>builder().message("Xoá bài viết thành công").build();
    }

    @Override
    public PageResponse<BlogResponse> getMyBlogs(int page, int size) {
        User shop = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<Blog> blogs = blogRepository.findByShop_Id(shop.getId(), pageable);
        return PageResponse.from(blogs, blogs.getContent().stream()
                .map(b -> mapToResponse(b, shop, shop)).toList());
    }

    // ===================== PUBLIC APIs =====================

    @Transactional
    @Override
    public BlogResponse getPublicBlogDetail(int blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        // Tăng lượt xem
        blog.setViews(blog.getViews() == null ? 1 : blog.getViews() + 1);
        blogRepository.save(blog);

        User currentUser = getCurrentUserOptionally();
        return mapToResponse(blog, blog.getShop(), currentUser);
    }

    @Override
    public PageResponse<BlogResponse> getPublicBlogs(int page, int size, BlogCategory category) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<Blog> blogs;
        if (category != null) {
            blogs = blogRepository.findByStatusAndCategory(BlogStatus.PUBLISHED, category, pageable);
        } else {
            blogs = blogRepository.findByStatus(BlogStatus.PUBLISHED, pageable);
        }
        User currentUser = getCurrentUserOptionally();
        return PageResponse.from(blogs, blogs.getContent().stream()
                .map(b -> mapToResponse(b, b.getShop(), currentUser)).toList());
    }

    // ===================== LIKE & COMMENT APIs =====================

    @Transactional
    @Override
    public ApiResponse<String> toggleLike(int blogId) {
        User user = getCurrentUser();
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        Optional<BlogLike> existingLike = blogLikeRepository.findByBlog_BlogIdAndUser_Id(blogId, user.getId());

        if (existingLike.isPresent()) {
            blogLikeRepository.delete(existingLike.get());
            return ApiResponse.<String>builder().message("Đã bỏ thích bài viết").result("UNLIKED").build();
        } else {
            BlogLike like = BlogLike.builder()
                    .blog(blog)
                    .user(user)
                    .createAt(LocalDateTime.now())
                    .build();
            blogLikeRepository.save(like);
            return ApiResponse.<String>builder().message("Đã thích bài viết").result("LIKED").build();
        }
    }

    @Transactional
    @Override
    public BlogCommentResponse addComment(int blogId, BlogCommentRequest request) {
        User user = getCurrentUser();
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        BlogComment comment = BlogComment.builder()
                .blog(blog)
                .user(user)
                .content(request.getContent())
                .createAt(LocalDateTime.now())
                .build();

        return mapToCommentResponse(blogCommentRepository.save(comment));
    }

    @Override
    public PageResponse<BlogCommentResponse> getComments(int blogId, int page, int size) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<BlogComment> comments = blogCommentRepository.findByBlog_BlogId(blogId, pageable);
        return PageResponse.from(comments, comments.getContent().stream()
                .map(this::mapToCommentResponse).toList());
    }

    @Transactional
    @Override
    public ApiResponse<Void> deleteComment(int commentId) {
        User user = getCurrentUser();
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        // Người bình luận hoặc chủ Shop bài viết đó đều có thể xóa
        boolean isCommentOwner = comment.getUser().getId() == user.getId();
        boolean isBlogOwner = comment.getBlog().getShop().getId() == user.getId();

        if (!isCommentOwner && !isBlogOwner) {
            throw new AppException(ErrorCode.NOT_COMMENT_OWNER);
        }

        blogCommentRepository.delete(comment);
        return ApiResponse.<Void>builder().message("Xoá bình luận thành công").build();
    }

    // ===================== HELPER METHODS =====================

    private BlogResponse mapToResponse(Blog blog, User shop, User currentUser) {
        long likeCount = blogLikeRepository.countByBlog_BlogId(blog.getBlogId());
        long commentCount = blogCommentRepository.countByBlog_BlogId(blog.getBlogId());

        // Kiểm tra user hiện tại đã like chưa (nếu đang đăng nhập)
        Boolean isLiked = null;
        if (currentUser != null) {
            isLiked = blogLikeRepository.findByBlog_BlogIdAndUser_Id(blog.getBlogId(), currentUser.getId()).isPresent();
        }

        return BlogResponse.builder()
                .blogId(blog.getBlogId())
                .title(blog.getTitle())
                .content(blog.getContent())
                .pictureUrl(blog.getPictureUrl())
                .status(blog.getStatus())
                .category(blog.getCategory())
                .createAt(blog.getCreateAt())
                .updatedAt(blog.getUpdatedAt())
                .views(blog.getViews())
                .shopId(shop.getId())
                .shopName(shop.getShopName() != null ? shop.getShopName() : shop.getFullName())
                .shopAvatar(shop.getLogoUrl())
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLikedByCurrentUser(isLiked)
                .build();
    }

    private BlogCommentResponse mapToCommentResponse(BlogComment comment) {
        User user = comment.getUser();
        return BlogCommentResponse.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createAt(comment.getCreateAt())
                .userId(user.getId())
                .userName(user.getFullName())
                .userAvatar(user.getLogoUrl())
                .build();
    }

    private User getCurrentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            int userId = Integer.parseInt(subject);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private User getCurrentUserOptionally() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        if (subject == null || subject.equals("anonymousUser")) {
            return null;
        }
        try {
            int userId = Integer.parseInt(subject);
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
