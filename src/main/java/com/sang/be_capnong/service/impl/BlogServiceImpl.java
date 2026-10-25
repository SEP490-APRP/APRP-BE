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
import com.sang.be_capnong.repository.specification.BlogSpecification;
import com.sang.be_capnong.service.BlogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BlogServiceImpl implements BlogService {

    BlogRepository blogRepository;
    BlogLikeRepository blogLikeRepository;
    BlogCommentRepository blogCommentRepository;
    UserRepository userRepository;
    SimpMessagingTemplate messagingTemplate;

    // ===================== CRUD APIs (SHOP & ADMIN) =====================

    @Transactional
    @Override
    public BlogResponse createBlog(BlogCreateRequest request) {
        User creator = getCurrentUser();
        Blog blog = Blog.builder()
                .shop(creator)
                .title(request.getTitle())
                .content(request.getContent())
                .pictureUrl(request.getPictureUrl())
                .status(request.getStatus())
                .category(request.getCategory())
                .createAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .views(0)
                .build();
        return mapToResponse(blogRepository.save(blog), creator, 0L, 0L, false);
    }

    @Transactional
    @Override
    public BlogResponse updateBlog(int blogId, BlogUpdateRequest request) {
        User currentUser = getCurrentUser();
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        boolean isOwner = blog.getShop() != null && Objects.equals(blog.getShop().getId(), currentUser.getId());
        boolean isAdmin = isAdmin(currentUser);

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.NOT_BLOG_OWNER);
        }

        if (request.getTitle() != null) blog.setTitle(request.getTitle());
        if (request.getContent() != null) blog.setContent(request.getContent());
        if (request.getPictureUrl() != null) blog.setPictureUrl(request.getPictureUrl());
        if (request.getStatus() != null) blog.setStatus(request.getStatus());
        if (request.getCategory() != null) blog.setCategory(request.getCategory());
        blog.setUpdatedAt(LocalDateTime.now());

        Blog saved = blogRepository.save(blog);
        long likeCount = blogLikeRepository.countByBlog_BlogId(blogId);
        long commentCount = blogCommentRepository.countByBlog_BlogId(blogId);
        Boolean isLiked = blogLikeRepository.findByBlog_BlogIdAndUser_Id(blogId, currentUser.getId()).isPresent();

        return mapToResponse(saved, saved.getShop(), likeCount, commentCount, isLiked);
    }

    @Transactional
    @Override
    public ApiResponse<Void> deleteBlog(int blogId) {
        User currentUser = getCurrentUser();
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        boolean isOwner = blog.getShop() != null && Objects.equals(blog.getShop().getId(), currentUser.getId());
        boolean isAdmin = isAdmin(currentUser);

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.NOT_BLOG_OWNER);
        }

        blogRepository.delete(blog);
        return ApiResponse.<Void>builder().message("Xoá bài viết thành công").build();
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<BlogResponse> getMyBlogs(int page, int size) {
        User shop = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Page<Blog> blogs = blogRepository.findByShop_Id(shop.getId(), pageable);
        return mapToBatchResponse(blogs, shop);
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
        long likeCount = blogLikeRepository.countByBlog_BlogId(blogId);
        long commentCount = blogCommentRepository.countByBlog_BlogId(blogId);
        Boolean isLiked = (currentUser != null)
                ? blogLikeRepository.findByBlog_BlogIdAndUser_Id(blogId, currentUser.getId()).isPresent()
                : null;

        return mapToResponse(blog, blog.getShop(), likeCount, commentCount, isLiked);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<BlogResponse> getPublicBlogs(int page, int size, BlogCategory category, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Specification<Blog> spec = BlogSpecification.filterBlogs(BlogStatus.PUBLISHED, category, search, null);
        Page<Blog> blogs = blogRepository.findAll(spec, pageable);
        User currentUser = getCurrentUserOptionally();
        return mapToBatchResponse(blogs, currentUser);
    }

    // ===================== ADMIN APIs =====================

    @Transactional(readOnly = true)
    @Override
    public PageResponse<BlogResponse> getAllBlogsForAdmin(int page, int size, BlogStatus status, BlogCategory category, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());
        Specification<Blog> spec = BlogSpecification.filterBlogs(status, category, search, null);
        Page<Blog> blogs = blogRepository.findAll(spec, pageable);
        User currentUser = getCurrentUserOptionally();
        return mapToBatchResponse(blogs, currentUser);
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
        String action;
        String message;

        if (existingLike.isPresent()) {
            blogLikeRepository.delete(existingLike.get());
            blogLikeRepository.flush();
            action = "UNLIKED";
            message = "Đã bỏ thích bài viết";
        } else {
            BlogLike like = BlogLike.builder()
                    .blog(blog)
                    .user(user)
                    .createAt(LocalDateTime.now())
                    .build();
            blogLikeRepository.saveAndFlush(like);
            action = "LIKED";
            message = "Đã thích bài viết";
        }

        long likeCount = blogLikeRepository.countByBlog_BlogId(blogId);
        long commentCount = blogCommentRepository.countByBlog_BlogId(blogId);

        broadcastBlogEvent(blogId, Map.of(
                "type", "LIKE_UPDATED",
                "blogId", blogId,
                "likeCount", likeCount,
                "commentCount", commentCount,
                "userId", user.getId(),
                "action", action
        ));

        return ApiResponse.<String>builder().message(message).result(action).build();
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

        BlogComment savedComment = blogCommentRepository.save(comment);
        BlogCommentResponse commentResponse = mapToCommentResponse(savedComment);

        long likeCount = blogLikeRepository.countByBlog_BlogId(blogId);
        long commentCount = blogCommentRepository.countByBlog_BlogId(blogId);

        broadcastBlogEvent(blogId, Map.of(
                "type", "COMMENT_ADDED",
                "blogId", blogId,
                "comment", commentResponse,
                "commentCount", commentCount,
                "likeCount", likeCount
        ));

        return commentResponse;
    }

    @Transactional(readOnly = true)
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
        User currentUser = getCurrentUser();
        BlogComment comment = blogCommentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isCommentOwner = comment.getUser() != null && Objects.equals(comment.getUser().getId(), currentUser.getId());
        boolean isBlogOwner = comment.getBlog() != null && comment.getBlog().getShop() != null && Objects.equals(comment.getBlog().getShop().getId(), currentUser.getId());
        boolean isAdmin = isAdmin(currentUser);

        if (!isCommentOwner && !isBlogOwner && !isAdmin) {
            throw new AppException(ErrorCode.NOT_COMMENT_OWNER);
        }

        int blogId = comment.getBlog().getBlogId();
        blogCommentRepository.delete(comment);

        long likeCount = blogLikeRepository.countByBlog_BlogId(blogId);
        long commentCount = blogCommentRepository.countByBlog_BlogId(blogId);

        broadcastBlogEvent(blogId, Map.of(
                "type", "COMMENT_DELETED",
                "blogId", blogId,
                "commentId", commentId,
                "commentCount", commentCount,
                "likeCount", likeCount
        ));

        return ApiResponse.<Void>builder().message("Xoá bình luận thành công").build();
    }

    private void broadcastBlogEvent(int blogId, Map<String, Object> payload) {
        Runnable broadcast = () -> {
            try {
                // Broadcast tới topic chi tiết của bài viết
                messagingTemplate.convertAndSend("/topic/blogs." + blogId, payload);
                // Broadcast tới topic chung toàn sàn (cho trang feed danh sách)
                messagingTemplate.convertAndSend("/topic/blogs", payload);
                log.info("[WS Blog] Broadcasted event {} for blogId={}", payload.get("type"), blogId);
            } catch (Exception e) {
                log.warn("[WS Blog] Failed to broadcast blog event: {}", e.getMessage());
            }
        };

        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            broadcast.run();
                        }
                    }
            );
        } else {
            broadcast.run();
        }
    }

    // ===================== HELPER METHODS =====================

    private PageResponse<BlogResponse> mapToBatchResponse(Page<Blog> blogs, User currentUser) {
        List<Blog> content = blogs.getContent();
        if (content.isEmpty()) {
            return PageResponse.from(blogs, Collections.emptyList());
        }

        List<Integer> blogIds = content.stream().map(Blog::getBlogId).toList();

        Map<Integer, Long> likeCountMap = blogLikeRepository.findByBlog_BlogIdIn(blogIds).stream()
                .collect(Collectors.groupingBy(l -> l.getBlog().getBlogId(), Collectors.counting()));

        Map<Integer, Long> commentCountMap = blogCommentRepository.findByBlog_BlogIdIn(blogIds).stream()
                .collect(Collectors.groupingBy(c -> c.getBlog().getBlogId(), Collectors.counting()));

        Set<Integer> likedBlogIds = (currentUser != null)
                ? blogLikeRepository.findByBlog_BlogIdInAndUser_Id(blogIds, currentUser.getId()).stream()
                        .map(l -> l.getBlog().getBlogId()).collect(Collectors.toSet())
                : Collections.emptySet();

        List<BlogResponse> dtoList = content.stream().map(blog -> {
            int bId = blog.getBlogId();
            long likes = likeCountMap.getOrDefault(bId, 0L);
            long comments = commentCountMap.getOrDefault(bId, 0L);
            Boolean isLiked = (currentUser != null) ? likedBlogIds.contains(bId) : null;
            return mapToResponse(blog, blog.getShop(), likes, comments, isLiked);
        }).toList();

        return PageResponse.from(blogs, dtoList);
    }

    private BlogResponse mapToResponse(Blog blog, User shop, long likeCount, long commentCount, Boolean isLiked) {
        int shopId = (shop != null) ? shop.getId() : 0;
        String shopName = "CapNong";
        if (shop != null) {
            if (shop.getShopName() != null && !shop.getShopName().isBlank()) {
                shopName = shop.getShopName();
            } else if (shop.getFullName() != null && !shop.getFullName().isBlank()) {
                shopName = shop.getFullName();
            } else if (isAdmin(shop)) {
                shopName = "Quản trị viên";
            }
        }
        String shopAvatar = (shop != null) ? shop.getLogoUrl() : null;

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
                .shopId(shopId)
                .shopName(shopName)
                .shopAvatar(shopAvatar)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLikedByCurrentUser(isLiked)
                .build();
    }

    private BlogCommentResponse mapToCommentResponse(BlogComment comment) {
        User user = comment.getUser();
        int userId = (user != null) ? user.getId() : 0;
        String userName = "Người dùng";
        if (user != null) {
            if (user.getFullName() != null && !user.getFullName().isBlank()) {
                userName = user.getFullName();
            } else if (isAdmin(user)) {
                userName = "Quản trị viên";
            }
        }
        String userAvatar = (user != null) ? user.getLogoUrl() : null;

        return BlogCommentResponse.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .createAt(comment.getCreateAt())
                .userId(userId)
                .userName(userName)
                .userAvatar(userAvatar)
                .build();
    }

    private boolean isAdmin(User user) {
        if (user == null) return false;
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
        }
        return false;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String subject = auth.getName();
        try {
            int userId = Integer.parseInt(subject);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private User getCurrentUserOptionally() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        String subject = auth.getName();
        try {
            int userId = Integer.parseInt(subject);
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
