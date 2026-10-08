package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.sang.be_capnong.dto.ModerationCheckResult;
import com.sang.be_capnong.enums.AgriculturalProduceLabel;
import com.sang.be_capnong.enums.HumanPresenceLabel;
import com.sang.be_capnong.enums.NonAgriculturalItem;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.service.ImageDownloadService;
import com.sang.be_capnong.service.ImageModerationService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class GoogleVisionModerationServiceImpl implements ImageModerationService {

    final ImageAnnotatorClient imageAnnotatorClient;
    final ObjectMapper objectMapper;
    final ImageDownloadService imageDownloadService;

    @Value("${google.vision.min-confidence:70.0}")
    float minConfidenceThreshold;

    @Value("${google.vision.non-agri-threshold:80.0}")
    float nonAgriThreshold;

    @Value("${google.vision.max-labels:30}")
    int maxLabels;

    @Autowired
    public GoogleVisionModerationServiceImpl(
            @Autowired(required = false) ImageAnnotatorClient imageAnnotatorClient,
            ObjectMapper objectMapper,
            ImageDownloadService imageDownloadService
    ) {
        this.imageAnnotatorClient = imageAnnotatorClient;
        this.objectMapper = objectMapper;
        this.imageDownloadService = imageDownloadService;
    }

    @Override
    public ModerationCheckResult checkImageUrl(String imageUrl) {
        log.info("Starting image moderation via Google Cloud Vision API for URL: {}", imageUrl);

        // Trường hợp credentials chưa được cấu hình -> Kích hoạt cơ chế Controlled Fail-Open
        if (imageAnnotatorClient == null) {
            log.warn("ImageAnnotatorClient is null (credentials not provided). Applying Controlled Fail-Open.");
            return buildFailOpenResult(new IllegalStateException("Google Cloud Vision credentials are not configured."));
        }

        try {
            // TRẠM 3: Phòng vệ an ninh SSRF 4 lớp (validate scheme, DNS rebinding, private IP, redirect never, <= 5MB)
            byte[] imageBytes = imageDownloadService.download(imageUrl);
            ByteString imgBytes = ByteString.copyFrom(imageBytes);

            // Xây dựng request đơn gộp cả LABEL_DETECTION và SAFE_SEARCH_DETECTION (Tiết kiệm 50% thời gian round-trip)
            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature featLabels = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .setMaxResults(maxLabels)
                    .build();
            Feature featSafeSearch = Feature.newBuilder()
                    .setType(Feature.Type.SAFE_SEARCH_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .setImage(img)
                    .addFeatures(featLabels)
                    .addFeatures(featSafeSearch)
                    .build();

            BatchAnnotateImagesResponse response = imageAnnotatorClient.batchAnnotateImages(List.of(request));
            if (response.getResponsesList().isEmpty()) {
                throw new RuntimeException("Empty response from Google Cloud Vision API");
            }

            AnnotateImageResponse annotateResponse = response.getResponses(0);
            if (annotateResponse.hasError()) {
                log.error("Google Cloud Vision API returned error: {}", annotateResponse.getError().getMessage());
                throw new RuntimeException(annotateResponse.getError().getMessage());
            }

            // BƯỚC 1: Phân tích nhãn nông sản (LABEL_DETECTION)
            float highestAgriConfidence = 0.0f;
            float highestNonAgriConfidence = 0.0f;
            String topNonAgriName = null;
            boolean hasHumanOrBodyPresence = false;

            List<ModerationCheckResult.ModerationLabelItem> matchedLabels = new ArrayList<>();
            List<Map<String, Object>> rawLabelList = new ArrayList<>();

            for (EntityAnnotation annotation : annotateResponse.getLabelAnnotationsList()) {
                String description = annotation.getDescription();
                float scorePercent = annotation.getScore() * 100.0f;

                Map<String, Object> rawItem = new HashMap<>();
                rawItem.put("name", description);
                rawItem.put("confidence", scorePercent);
                rawLabelList.add(rawItem);

                // 1. Kiểm tra dấu hiệu con người / cơ thể
                if (HumanPresenceLabel.isHumanPresence(description)) {
                    hasHumanOrBodyPresence = true;
                }

                // 2. Kiểm tra nhãn nông sản
                if (AgriculturalProduceLabel.isAgricultural(description)) {
                    if (scorePercent > highestAgriConfidence) {
                        highestAgriConfidence = scorePercent;
                    }
                    matchedLabels.add(ModerationCheckResult.ModerationLabelItem.builder()
                            .labelName(description)
                            .parentLabel("GoogleVision")
                            .confidence(BigDecimal.valueOf(scorePercent).setScale(2, RoundingMode.HALF_UP))
                            .build());
                }

                // 3. Kiểm tra thức ăn nhanh / thực phẩm chế biến sẵn
                if (NonAgriculturalItem.isNonAgricultural(description)) {
                    if (scorePercent > highestNonAgriConfidence) {
                        highestNonAgriConfidence = scorePercent;
                        topNonAgriName = description;
                    }
                }
            }

            log.info("Google Vision labels: count={}, highestAgriConfidence={}%, hasHumanPresence={}",
                    annotateResponse.getLabelAnnotationsList().size(), highestAgriConfidence, hasHumanOrBodyPresence);

            // BƯỚC 2: Kiểm duyệt an toàn nội dung (SAFE_SEARCH_DETECTION)
            SafeSearchAnnotation safeSearch = annotateResponse.getSafeSearchAnnotation();
            if (safeSearch != null) {
                Optional<ModerationCheckResult> unsafeResult = evaluateSafeSearch(safeSearch, highestAgriConfidence);
                if (unsafeResult.isPresent()) {
                    return unsafeResult.get();
                }
            }

            // BƯỚC 3: Đánh giá chất lượng nông sản
            // Nhánh 3.1: Thức ăn nhanh / chế biến sẵn
            if (highestNonAgriConfidence >= nonAgriThreshold && highestAgriConfidence < minConfidenceThreshold) {
                String nonAgriVi = NonAgriculturalItem.getVietnameseNameOrDefault(topNonAgriName);
                return ModerationCheckResult.builder()
                        .isAgricultural(false)
                        .isFlagged(true)
                        .overallConfidence(BigDecimal.valueOf(highestNonAgriConfidence).setScale(2, RoundingMode.HALF_UP))
                        .rejectionReason("Ảnh sản phẩm là thực phẩm chế biến sẵn hoặc đồ uống (" + nonAgriVi + "), không phải nông sản tươi sống giải cứu.")
                        .rawResponse(objectMapper.writeValueAsString(rawLabelList))
                        .labels(matchedLabels)
                        .build();
            }

            // Nhánh 3.2: Đánh giá nông sản
            boolean isAgricultural = highestAgriConfidence >= minConfidenceThreshold;
            String rejectionReason = isAgricultural ? null
                    : "Ảnh sản phẩm không phải là nông sản hoặc không đạt độ rõ nét yêu cầu (Độ tin cậy: "
                    + String.format("%.1f", highestAgriConfidence) + "% < ngưỡng " + minConfidenceThreshold + "%)";

            return ModerationCheckResult.builder()
                    .isAgricultural(isAgricultural)
                    .isFlagged(!isAgricultural)
                    .overallConfidence(BigDecimal.valueOf(highestAgriConfidence).setScale(2, RoundingMode.HALF_UP))
                    .rejectionReason(rejectionReason)
                    .rawResponse(objectMapper.writeValueAsString(rawLabelList))
                    .labels(matchedLabels)
                    .build();

        } catch (AppException e) {
            // Lỗi nghiệp vụ SSRF hoặc file invalid từ ImageDownloadService -> Ném ra ngoài, KHÔNG fail-open
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while moderating image via Google Cloud Vision: {}. Applying Fail-Open mechanism.",
                    e.getMessage(), e);
            return buildFailOpenResult(e);
        }
    }

    private Optional<ModerationCheckResult> evaluateSafeSearch(SafeSearchAnnotation safeSearch, float agriConfidence) {
        Likelihood adult = safeSearch.getAdult();
        Likelihood violence = safeSearch.getViolence();
        Likelihood racy = safeSearch.getRacy();

        boolean isAdult = adult == Likelihood.LIKELY || adult == Likelihood.VERY_LIKELY;
        boolean isViolence = violence == Likelihood.LIKELY || violence == Likelihood.VERY_LIKELY;

        if (isAdult) {
            log.warn("Image rejected by Google SafeSearch: Adult content detected ({})", adult);
            return Optional.of(ModerationCheckResult.builder()
                    .isAgricultural(false)
                    .isFlagged(true)
                    .overallConfidence(BigDecimal.valueOf(95.0).setScale(2, RoundingMode.HALF_UP))
                    .rejectionReason("Ảnh sản phẩm chứa nội dung không phù hợp hoặc nhạy cảm người lớn (SafeSearch: Adult)")
                    .rawResponse("{\"adult\":\"" + adult.name() + "\"}")
                    .labels(Collections.emptyList())
                    .build());
        }

        if (isViolence) {
            log.warn("Image rejected by Google SafeSearch: Violence detected ({})", violence);
            return Optional.of(ModerationCheckResult.builder()
                    .isAgricultural(false)
                    .isFlagged(true)
                    .overallConfidence(BigDecimal.valueOf(95.0).setScale(2, RoundingMode.HALF_UP))
                    .rejectionReason("Ảnh sản phẩm chứa nội dung bạo lực hoặc gây phản cảm (SafeSearch: Violence)")
                    .rawResponse("{\"violence\":\"" + violence.name() + "\"}")
                    .labels(Collections.emptyList())
                    .build());
        }

        // Cơ chế chống oan sai (False-positive quả chuối / cà tím):
        // Nếu nông sản rất rõ nét (agriConfidence >= 85%) và racy ở mức LIKELY -> Không chặn oan người nông dân
        if (racy == Likelihood.LIKELY || racy == Likelihood.VERY_LIKELY) {
            if (agriConfidence >= 85.0f && racy == Likelihood.LIKELY) {
                log.info("Detected suggestive racy flag ({}) on high-confidence agricultural produce ({}%). Ignoring false positive.",
                        racy, agriConfidence);
                return Optional.empty();
            }

            log.warn("Image rejected by Google SafeSearch: Racy content detected ({})", racy);
            return Optional.of(ModerationCheckResult.builder()
                    .isAgricultural(false)
                    .isFlagged(true)
                    .overallConfidence(BigDecimal.valueOf(85.0).setScale(2, RoundingMode.HALF_UP))
                    .rejectionReason("Ảnh sản phẩm chứa nội dung nhạy cảm hoặc gợi cảm (SafeSearch: Racy)")
                    .rawResponse("{\"racy\":\"" + racy.name() + "\"}")
                    .labels(Collections.emptyList())
                    .build());
        }

        return Optional.empty();
    }

    private ModerationCheckResult buildFailOpenResult(Exception e) {
        return ModerationCheckResult.builder()
                .isAgricultural(true)
                .isFlagged(true)
                .overallConfidence(BigDecimal.valueOf(75.0).setScale(2, RoundingMode.HALF_UP))
                .rejectionReason("Hệ thống AI kiểm duyệt tạm thời gián đoạn, ảnh đã được lưu và đưa vào hàng đợi kiểm duyệt thủ công của Quản trị viên.")
                .rawResponse("FAIL_OPEN_FALLBACK: " + e.getMessage())
                .labels(Collections.emptyList())
                .build();
    }
}
