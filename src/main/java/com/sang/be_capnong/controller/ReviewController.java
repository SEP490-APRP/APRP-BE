package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ReviewCreateRequest;
import com.sang.be_capnong.dto.request.ReviewReactionRequest;
import com.sang.be_capnong.dto.request.ReviewReplyRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ReviewResponse;
import com.sang.be_capnong.dto.response.ReviewShopReport;
import com.sang.be_capnong.service.ReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReviewController {

    ReviewService reviewService;

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@RequestBody @Valid ReviewCreateRequest request) {
        log.info("Create review request received for orderDetailId={}", request.getOrderDetailId());
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.createReview(request))
                .build();
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<ReviewResponse> replyReview(
            @PathVariable("id") int id,
            @RequestBody @Valid ReviewReplyRequest request
    ) {
        log.info("Shop replying to review id={}", id);
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.replyReview(id, request))
                .build();
    }

    @PostMapping("/{id}/react")
    public ApiResponse<ReviewResponse> reactToReview(
            @PathVariable("id") int id,
            @RequestBody @Valid ReviewReactionRequest request
    ) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.reactToReview(id, request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewResponse> getById(@PathVariable("id") int id) {
        return ApiResponse.<ReviewResponse>builder()
                .result(reviewService.getReviewById(id))
                .build();
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<List<ReviewResponse>> getByProductId(@PathVariable("productId") int productId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByProduct(productId))
                .build();
    }

    @GetMapping("/product/{productId}/paged")
    public ApiResponse<PageResponse<ReviewResponse>> getByProductIdPaged(
            @PathVariable("productId") int productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getReviewsByProductPaged(productId, page, size))
                .build();
    }

    @GetMapping("/shop/{shopId}")
    public ApiResponse<List<ReviewResponse>> getByShopId(@PathVariable("shopId") int shopId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByShop(shopId))
                .build();
    }

    @GetMapping("/shop/{shopId}/paged")
    public ApiResponse<PageResponse<ReviewResponse>> getByShopIdPaged(
            @PathVariable("shopId") int shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getReviewsByShopPaged(shopId, page, size))
                .build();
    }

    @GetMapping("/my-shop")
    public ApiResponse<PageResponse<ReviewResponse>> getMyShopReviewsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getMyShopReviewsPaged(page, size))
                .build();
    }

    @GetMapping("/mystery-box/{mysteryBoxId}")
    public ApiResponse<List<ReviewResponse>> getByMysteryBoxId(@PathVariable("mysteryBoxId") int mysteryBoxId) {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getReviewsByMysteryBox(mysteryBoxId))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<ReviewResponse>> getMyReviews() {
        return ApiResponse.<List<ReviewResponse>>builder()
                .result(reviewService.getMyReviews())
                .build();
    }

    @GetMapping("/my/paged")
    public ApiResponse<PageResponse<ReviewResponse>> getMyReviewsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getMyReviewsPaged(page, size))
                .build();
    }

    @GetMapping("/shop/report")
    public ApiResponse<ReviewShopReport> getMyShopReport() {
        return ApiResponse.<ReviewShopReport>builder()
                .result(reviewService.getMyShopReport())
                .build();
    }

    @GetMapping("/shop/{shopId}/report")
    public ApiResponse<ReviewShopReport> getShopReport(@PathVariable("shopId") int shopId) {
        return ApiResponse.<ReviewShopReport>builder()
                .result(reviewService.getShopReport(shopId))
                .build();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ReviewResponse>> getAllReviewsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<ReviewResponse>>builder()
                .result(reviewService.getAllReviewsAdmin(page, size))
                .build();
    }
}
