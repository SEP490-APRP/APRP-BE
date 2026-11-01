package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ReviewCreateRequest;
import com.sang.be_capnong.dto.request.ReviewReactionRequest;
import com.sang.be_capnong.dto.request.ReviewReplyRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ReviewResponse;
import com.sang.be_capnong.dto.response.ReviewShopReport;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(ReviewCreateRequest request);

    ReviewResponse replyReview(int reviewId, ReviewReplyRequest request);

    ReviewResponse reactToReview(int reviewId, ReviewReactionRequest request);

    ReviewResponse getReviewById(int reviewId);

    List<ReviewResponse> getReviewsByProduct(int productId);

    PageResponse<ReviewResponse> getReviewsByProductPaged(int productId, int page, int size);

    List<ReviewResponse> getReviewsByShop(int shopId);

    PageResponse<ReviewResponse> getReviewsByShopPaged(int shopId, int page, int size);

    PageResponse<ReviewResponse> getMyShopReviewsPaged(int page, int size);

    List<ReviewResponse> getReviewsByMysteryBox(int mysteryId);

    List<ReviewResponse> getMyReviews();

    PageResponse<ReviewResponse> getMyReviewsPaged(int page, int size);

    ReviewShopReport getShopReport(int shopId);

    ReviewShopReport getMyShopReport();

    PageResponse<ReviewResponse> getAllReviewsAdmin(int page, int size);
}
