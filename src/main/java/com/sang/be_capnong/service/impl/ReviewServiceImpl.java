package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ReviewCreateRequest;
import com.sang.be_capnong.dto.request.ReviewReactionRequest;
import com.sang.be_capnong.dto.request.ReviewReplyRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ReviewResponse;
import com.sang.be_capnong.dto.response.ReviewShopReport;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.MediaType;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.ReactionType;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ReviewMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.ReviewService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    ReviewRepository reviewRepository;
    ReviewReactionRepository reviewReactionRepository;
    OrderDetailRepository orderDetailRepository;
    UserRepository userRepository;
    MysteryBoxRepository mysteryBoxRepository;
    ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getRatingStar() == null
                || request.getRatingStar().compareTo(BigDecimal.ONE) < 0
                || request.getRatingStar().compareTo(BigDecimal.valueOf(5)) > 0) {
            throw new AppException(ErrorCode.INVALID_RATING);
        }

        Review review = reviewMapper.toReview(request);
        review.setBuyer(buyer);
        review.setCreateAt(LocalDateTime.now());

        User shopOwner = null;

        // 1. Review theo OrderDetail (Product, MysteryBox, or Combo item)
        if (request.getOrderDetailId() != null) {
            OrderDetail orderDetail = orderDetailRepository.findById(request.getOrderDetailId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_DETAIL_NOT_FOUND));

            Order order = orderDetail.getOrder();
            if (order == null) {
                throw new AppException(ErrorCode.ORDER_NOT_FOUND);
            }

            // Phải giao hàng thành công mới được đánh giá
            if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
                throw new AppException(ErrorCode.ORDER_NOT_DELIVERED);
            }

            // Phải chính là buyer sở hữu đơn hàng
            if (order.getBuyer() == null || order.getBuyer().getId() != buyer.getId()) {
                throw new AppException(ErrorCode.NOT_ORDER_BUYER);
            }

            // Chống spam: 1 order detail chỉ được đánh giá 1 lần
            if (reviewRepository.existsByOrderDetail_OrderDetailId(orderDetail.getOrderDetailId())) {
                throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }

            review.setOrderDetail(orderDetail);
            review.setProduct(orderDetail.getProduct());
            review.setMysteryBox(orderDetail.getMysteryBox());
            review.setBuildCombo(orderDetail.getBuildCombo());

            if (order.getShop() != null) {
                shopOwner = order.getShop();
            } else if (orderDetail.getProduct() != null && orderDetail.getProduct().getShop() != null) {
                shopOwner = orderDetail.getProduct().getShop();
            } else if (orderDetail.getMysteryBox() != null && orderDetail.getMysteryBox().getShop() != null) {
                shopOwner = orderDetail.getMysteryBox().getShop();
            } else if (orderDetail.getBuildCombo() != null && orderDetail.getBuildCombo().getShop() != null) {
                shopOwner = orderDetail.getBuildCombo().getShop();
            }

            review.setShop(shopOwner);
        }
        // 2. Review theo MysteryBoxId trực tiếp: Bắt buộc Buyer phải có đơn hàng đã giao (DELIVERED/COMPLETED) chứa MysteryBox này
        else if (request.getMysteryBoxId() != null) {
            MysteryBox mysteryBox = mysteryBoxRepository.findById(request.getMysteryBoxId())
                    .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

            // Tìm các đơn hàng đã giao thành công của Buyer có chứa MysteryBox này
            List<OrderDetail> deliveredDetails = orderDetailRepository.findDeliveredDetailsByMysteryBoxAndBuyer(
                    mysteryBox.getMysteryId(),
                    buyer.getId(),
                    List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED)
            );

            if (deliveredDetails.isEmpty()) {
                throw new AppException(ErrorCode.ORDER_NOT_DELIVERED);
            }

            // Tìm chi tiết đơn hàng chưa từng được đánh giá
            OrderDetail eligibleDetail = deliveredDetails.stream()
                    .filter(d -> !reviewRepository.existsByOrderDetail_OrderDetailId(d.getOrderDetailId()))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.REVIEW_ALREADY_EXISTS));

            review.setOrderDetail(eligibleDetail);
            review.setMysteryBox(mysteryBox);
            shopOwner = eligibleDetail.getOrder() != null && eligibleDetail.getOrder().getShop() != null
                    ? eligibleDetail.getOrder().getShop()
                    : mysteryBox.getShop();
            review.setShop(shopOwner);
        } else {
            throw new AppException(ErrorCode.INVALID_REVIEW_TARGET);
        }

        // Handle Images
        List<Image> imageEntities = new ArrayList<>();
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String url : request.getImageUrls()) {
                if (url != null && !url.isBlank()) {
                    imageEntities.add(Image.builder()
                            .imageUrl(url)
                            .mediaType(MediaType.PHOTO)
                            .createdAt(LocalDateTime.now())
                            .review(review)
                            .build());
                }
            }
        }
        if (!imageEntities.isEmpty()) {
            review.setImages(imageEntities);
            if (review.getEvidence() == null || review.getEvidence().isBlank()) {
                review.setEvidence(imageEntities.get(0).getImageUrl());
            }
        } else if (request.getEvidence() != null && !request.getEvidence().isBlank()) {
            review.setEvidence(request.getEvidence());
        }

        Review savedReview = reviewRepository.save(review);

        // Update Shop rating average
        if (shopOwner != null) {
            updateShopRating(shopOwner);
        }

        return toReviewResponseWithReactions(savedReview, buyer.getId());
    }

    private void updateShopRating(User shopOwner) {
        Double avg = reviewRepository.getAverageRatingByShopId(shopOwner.getId());
        double rounded = (avg != null && avg > 0) ? Math.round(avg * 10.0) / 10.0 : 0.0;
        shopOwner.setRatingAverage(rounded);
        userRepository.save(shopOwner);
    }

    @Override
    @Transactional
    public ReviewResponse replyReview(int reviewId, ReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (review.getShop() == null || review.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_REVIEW_SHOP);
        }

        review.setReplyFromShop(request.getReplyFromShop());
        Review saved = reviewRepository.save(review);
        return toReviewResponseWithReactions(saved, currentUserId);
    }

    @Override
    @Transactional
    public ReviewResponse reactToReview(int reviewId, ReviewReactionRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        Integer currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        reviewReactionRepository.findByReviewAndBuyer(review, currentUser)
                .ifPresentOrElse(
                        existing -> {
                            if (existing.getReactionType() == request.getReactionType()) {
                                reviewReactionRepository.delete(existing);
                            } else {
                                existing.setReactionType(request.getReactionType());
                                existing.setUpdatedAt(LocalDateTime.now());
                                reviewReactionRepository.save(existing);
                            }
                        },
                        () -> {
                            ReviewReaction newReaction = ReviewReaction.builder()
                                    .review(review)
                                    .buyer(currentUser)
                                    .reactionType(request.getReactionType())
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            reviewReactionRepository.save(newReaction);
                        }
                );

        return toReviewResponseWithReactions(review, currentUserId);
    }

    @Override
    public ReviewResponse getReviewById(int reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        return toReviewResponseWithReactions(review, currentUserId);
    }

    @Override
    public List<ReviewResponse> getReviewsByProduct(int productId) {
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        return reviewRepository.findByProduct_ProductId(productId)
                .stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
    }

    @Override
    public PageResponse<ReviewResponse> getReviewsByProductPaged(int productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewId").descending());
        Page<Review> pageResult = reviewRepository.findByProduct_ProductId(productId, pageable);
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        List<ReviewResponse> dtoList = pageResult.getContent().stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
        return PageResponse.from(pageResult, dtoList);
    }

    @Override
    public List<ReviewResponse> getReviewsByShop(int shopId) {
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        return reviewRepository.findByShop_Id(shopId)
                .stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
    }

    @Override
    public PageResponse<ReviewResponse> getReviewsByShopPaged(int shopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewId").descending());
        Page<Review> pageResult = reviewRepository.findByShop_Id(shopId, pageable);
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        List<ReviewResponse> dtoList = pageResult.getContent().stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
        return PageResponse.from(pageResult, dtoList);
    }

    @Override
    public PageResponse<ReviewResponse> getMyShopReviewsPaged(int page, int size) {
        Integer shopId = SecurityUtils.getCurrentUserId();
        return getReviewsByShopPaged(shopId, page, size);
    }

    @Override
    public List<ReviewResponse> getReviewsByMysteryBox(int mysteryId) {
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        return reviewRepository.findByMysteryBox_MysteryId(mysteryId)
                .stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
    }

    @Override
    public List<ReviewResponse> getMyReviews() {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        return reviewRepository.findByBuyer_Id(currentUserId)
                .stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
    }

    @Override
    public PageResponse<ReviewResponse> getMyReviewsPaged(int page, int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewId").descending());
        Page<Review> pageResult = reviewRepository.findByBuyer_Id(currentUserId, pageable);
        List<ReviewResponse> dtoList = pageResult.getContent().stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
        return PageResponse.from(pageResult, dtoList);
    }

    @Override
    public ReviewShopReport getShopReport(int shopId) {
        Double avg = reviewRepository.getAverageRatingByShopId(shopId);
        double rounded = (avg != null && avg > 0) ? Math.round(avg * 10.0) / 10.0 : 0.0;

        Long total = reviewRepository.countByShop_Id(shopId);
        long fiveStar = reviewRepository.countByShopIdAndRatingStar(shopId, BigDecimal.valueOf(5));
        long fourStar = reviewRepository.countByShopIdAndRatingStar(shopId, BigDecimal.valueOf(4));
        long threeStar = reviewRepository.countByShopIdAndRatingStar(shopId, BigDecimal.valueOf(3));
        long twoStar = reviewRepository.countByShopIdAndRatingStar(shopId, BigDecimal.valueOf(2));
        long oneStar = reviewRepository.countByShopIdAndRatingStar(shopId, BigDecimal.valueOf(1));

        return ReviewShopReport.builder()
                .averageRating(rounded)
                .totalReviewers(total != null ? total : 0)
                .fiveStarNumber(fiveStar)
                .fourStarNumber(fourStar)
                .threeStarNumber(threeStar)
                .twoStarNumber(twoStar)
                .oneStarNumber(oneStar)
                .build();
    }

    @Override
    public ReviewShopReport getMyShopReport() {
        Integer shopId = SecurityUtils.getCurrentUserId();
        return getShopReport(shopId);
    }

    @Override
    public PageResponse<ReviewResponse> getAllReviewsAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reviewId").descending());
        Page<Review> pageResult = reviewRepository.findAll(pageable);
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        List<ReviewResponse> dtoList = pageResult.getContent().stream()
                .map(r -> toReviewResponseWithReactions(r, currentUserId))
                .toList();
        return PageResponse.from(pageResult, dtoList);
    }

    private ReviewResponse toReviewResponseWithReactions(Review review, Integer currentUserId) {
        ReviewResponse response = reviewMapper.toReviewResponse(review);
        response.setLikeCount(reviewReactionRepository.countByReviewAndReactionType(review, ReactionType.LIKE));
        response.setDislikeCount(reviewReactionRepository.countByReviewAndReactionType(review, ReactionType.DISLIKE));

        if (currentUserId != null) {
            reviewReactionRepository.findByReview_ReviewIdAndBuyer_Id(review.getReviewId(), currentUserId)
                    .ifPresent(r -> response.setCurrentUserReaction(r.getReactionType().name()));
        }

        return response;
    }
}
