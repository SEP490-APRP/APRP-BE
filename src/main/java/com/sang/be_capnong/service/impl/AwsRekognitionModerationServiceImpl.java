package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.dto.ModerationCheckResult;
import com.sang.be_capnong.enums.AgriculturalProduceLabel;
import com.sang.be_capnong.enums.HumanPresenceLabel;
import com.sang.be_capnong.enums.NonAgriculturalItem;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.service.ImageDownloadService;
import com.sang.be_capnong.service.ImageModerationService;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AwsRekognitionModerationServiceImpl implements ImageModerationService {

    final RekognitionClient rekognitionClient;
    final ObjectMapper objectMapper;
    final ImageDownloadService imageDownloadService;

    @Value("${aws.moderation.min-confidence:75.0}")
    float minConfidenceThreshold;

    @Value("${aws.moderation.unsafe-min-confidence:60.0}")
    float unsafeMinConfidence;

    @Value("${aws.moderation.non-agri-threshold:80.0}")
    float nonAgriThreshold;

    @Value("${aws.moderation.max-labels:30}")
    int maxLabels;

    @Value("${aws.moderation.label-min-confidence:50.0}")
    float labelMinConfidence;

    @Override
    public ModerationCheckResult checkImageUrl(String imageUrl) {
        log.info("Starting Hybrid Smart image moderation via AWS Rekognition for URL: {}", imageUrl);

        try {
            byte[] imageBytes = imageDownloadService.download(imageUrl);
            SdkBytes sdkBytes = SdkBytes.fromByteArray(imageBytes);
            Image image = Image.builder().bytes(sdkBytes).build();

            // BƯỚC 1: Quét nhận diện toàn bộ vật thể trong ảnh trước (detectLabels)
            LabelEvaluationResult evaluation = detectProduceLabels(image);

            // BƯỚC 2: PHÂN NHÁNH XỬ LÝ THÔNG MINH (HYBRID SMART INSPECTION)

            // Nhánh 2.1: Ảnh đồ vật vô tri (xe cộ, đồ đạc, máy móc, phòng ốc)
            // Không đạt chuẩn nông sản VÀ hoàn toàn không có dấu hiệu con người/cơ thể
            if (evaluation.getHighestAgriConfidence() < minConfidenceThreshold && !evaluation.isHasHumanOrBodyPresence()) {
                log.info("Inanimate non-agricultural object detected. Short-circuiting moderation check to save 50% cost.");
                return evaluateResult(evaluation);
            }

            // Nhánh 2.2: Ảnh có dấu hiệu con người (nghi vấn 18+) HOẶC ảnh đạt chuẩn nông sản
            // Bắt buộc gọi detectModerationLabels để thẩm định an toàn nội dung
            Optional<ModerationCheckResult> unsafeResult = checkUnsafeContent(image, evaluation.getHighestAgriConfidence());
            if (unsafeResult.isPresent()) {
                return unsafeResult.get();
            }

            // Nhánh 2.3: Ảnh an toàn, đánh giá tiếp kết quả nông sản
            return evaluateResult(evaluation);

        } catch (AppException e) {
            // Lỗi nghiệp vụ từ downloader (URL sai, SSRF, file quá lớn...) -> Ném ra ngoài, KHÔNG fail-open
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while moderating image via AWS Rekognition: {}. Applying Fail-Open mechanism.",
                    e.getMessage(), e);
            return buildFailOpenResult(e);
        }
    }

    private Optional<ModerationCheckResult> checkUnsafeContent(Image image, float agriConfidence) throws JsonProcessingException {
        DetectModerationLabelsRequest moderationRequest = DetectModerationLabelsRequest.builder()
                .image(image)
                .minConfidence(unsafeMinConfidence)
                .build();

        DetectModerationLabelsResponse moderationResponse = rekognitionClient.detectModerationLabels(moderationRequest);
        if (!moderationResponse.hasModerationLabels() || moderationResponse.moderationLabels().isEmpty()) {
            return Optional.empty();
        }

        software.amazon.awssdk.services.rekognition.model.ModerationLabel topUnsafeLabel =
                moderationResponse.moderationLabels().get(0);

        // Cơ chế chống oan sai (Nghịch lý Quả Chuối / Cà Tím):
        // Nếu nông sản rất rõ nét (agriConfidence >= 90%) và nhãn nhạy cảm chỉ là "Suggestive" ở mức nhẹ (< 75%)
        // mà KHÔNG phải là "Explicit Nudity" hay "Violence" -> Không chặn oan người nông dân!
        String topLabelName = topUnsafeLabel.name().toUpperCase();
        boolean isExplicitOrViolent = topLabelName.contains("EXPLICIT") || topLabelName.contains("NUDITY")
                || topLabelName.contains("VIOLENCE") || topLabelName.contains("VISUALLY DISTURBING");

        if (!isExplicitOrViolent && agriConfidence >= 90.0f && topUnsafeLabel.confidence() < 75.0f) {
            log.info("Detected mild suggestive flag ({}: {}%) on highly confident agricultural produce ({}%). Ignoring false positive.",
                    topUnsafeLabel.name(), topUnsafeLabel.confidence(), agriConfidence);
            return Optional.empty();
        }

        log.warn("Image rejected due to unsafe content: {} (confidence: {}%)",
                topUnsafeLabel.name(), topUnsafeLabel.confidence());

        List<Map<String, Object>> rawModList = new ArrayList<>();
        for (software.amazon.awssdk.services.rekognition.model.ModerationLabel modLabel : moderationResponse.moderationLabels()) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", modLabel.name());
            item.put("confidence", modLabel.confidence());
            if (modLabel.parentName() != null) {
                item.put("parentName", modLabel.parentName());
            }
            rawModList.add(item);
        }

        return Optional.of(ModerationCheckResult.builder()
                .isAgricultural(false)
                .isFlagged(true)
                .overallConfidence(BigDecimal.valueOf(topUnsafeLabel.confidence()).setScale(2, RoundingMode.HALF_UP))
                .rejectionReason("Ảnh sản phẩm chứa nội dung không phù hợp hoặc nhạy cảm (" + topUnsafeLabel.name() + ")")
                .rawResponse(objectMapper.writeValueAsString(rawModList))
                .labels(Collections.emptyList())
                .build());
    }

    private LabelEvaluationResult detectProduceLabels(Image image) {
        DetectLabelsRequest labelsRequest = DetectLabelsRequest.builder()
                .image(image)
                .maxLabels(maxLabels)
                .minConfidence(labelMinConfidence)
                .build();

        DetectLabelsResponse labelsResponse = rekognitionClient.detectLabels(labelsRequest);

        float highestAgriConfidence = 0.0f;
        float highestNonAgriConfidence = 0.0f;
        String topNonAgriName = null;
        boolean hasHumanOrBodyPresence = false;

        List<ModerationCheckResult.ModerationLabelItem> matchedLabels = new ArrayList<>();
        List<Map<String, Object>> rawLabelList = new ArrayList<>();

        for (Label label : labelsResponse.labels()) {
            String labelName = label.name();
            float confidence = label.confidence() != null ? label.confidence() : 0.0f;

            Map<String, Object> rawItem = new HashMap<>();
            rawItem.put("name", labelName);
            rawItem.put("confidence", confidence);
            if (label.hasParents() && !label.parents().isEmpty()) {
                rawItem.put("parent", label.parents().get(0).name());
            }
            rawLabelList.add(rawItem);

            // 1. Kiểm tra dấu hiệu con người / cơ thể
            if (isHumanPresenceLabel(label)) {
                hasHumanOrBodyPresence = true;
            }

            // 2. Kiểm tra xem nhãn có thuộc danh mục nông sản
            if (isAgriculturalLabel(label)) {
                if (confidence > highestAgriConfidence) {
                    highestAgriConfidence = confidence;
                }
                String parentName = label.hasParents() && !label.parents().isEmpty()
                        ? label.parents().get(0).name() : null;

                matchedLabels.add(ModerationCheckResult.ModerationLabelItem.builder()
                        .labelName(labelName)
                        .parentLabel(parentName)
                        .confidence(BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP))
                        .build());
            }

            // 3. Kiểm tra xem có chứa thức ăn nhanh / món chế biến sẵn
            if (NonAgriculturalItem.isNonAgricultural(labelName)) {
                if (confidence > highestNonAgriConfidence) {
                    highestNonAgriConfidence = confidence;
                    topNonAgriName = labelName;
                }
            }
        }

        log.info("AWS Rekognition labels detected: count={}, highestAgriConfidence={}%, hasHumanPresence={}",
                labelsResponse.labels().size(), highestAgriConfidence, hasHumanOrBodyPresence);

        return LabelEvaluationResult.builder()
                .highestAgriConfidence(highestAgriConfidence)
                .highestNonAgriConfidence(highestNonAgriConfidence)
                .topNonAgriName(topNonAgriName)
                .hasHumanOrBodyPresence(hasHumanOrBodyPresence)
                .matchedLabels(matchedLabels)
                .rawLabelList(rawLabelList)
                .build();
    }

    private boolean isHumanPresenceLabel(Label label) {
        if (HumanPresenceLabel.isHumanPresence(label.name())) {
            return true;
        }

        if (label.hasParents()) {
            for (Parent parent : label.parents()) {
                if (HumanPresenceLabel.isHumanPresence(parent.name())) {
                    return true;
                }
            }
        }

        if (label.hasCategories()) {
            for (LabelCategory category : label.categories()) {
                if (HumanPresenceLabel.isHumanPresence(category.name())) {
                    return true;
                }
            }
        }

        return false;
    }

    private ModerationCheckResult evaluateResult(LabelEvaluationResult eval) throws JsonProcessingException {
        // Nếu phát hiện món ăn chế biến sẵn / thức ăn nhanh với độ tin cậy cao và không có nông sản đối ứng
        if (eval.getHighestNonAgriConfidence() >= nonAgriThreshold && eval.getHighestAgriConfidence() < minConfidenceThreshold) {
            String nonAgriVi = NonAgriculturalItem.getVietnameseNameOrDefault(eval.getTopNonAgriName());
            return ModerationCheckResult.builder()
                    .isAgricultural(false)
                    .isFlagged(true)
                    .overallConfidence(BigDecimal.valueOf(eval.getHighestNonAgriConfidence()).setScale(2, RoundingMode.HALF_UP))
                    .rejectionReason("Ảnh sản phẩm là thực phẩm chế biến sẵn hoặc đồ uống (" + nonAgriVi + "), không phải nông sản tươi sống giải cứu.")
                    .rawResponse(objectMapper.writeValueAsString(eval.getRawLabelList()))
                    .labels(eval.getMatchedLabels())
                    .build();
        }

        boolean isAgricultural = eval.getHighestAgriConfidence() >= minConfidenceThreshold;
        String rejectionReason = isAgricultural ? null
                : "Ảnh sản phẩm không phải là nông sản hoặc không đạt độ rõ nét yêu cầu (Độ tin cậy: "
                + String.format("%.1f", eval.getHighestAgriConfidence()) + "% < ngưỡng " + minConfidenceThreshold + "%)";

        return ModerationCheckResult.builder()
                .isAgricultural(isAgricultural)
                .isFlagged(!isAgricultural)
                .overallConfidence(BigDecimal.valueOf(eval.getHighestAgriConfidence()).setScale(2, RoundingMode.HALF_UP))
                .rejectionReason(rejectionReason)
                .rawResponse(objectMapper.writeValueAsString(eval.getRawLabelList()))
                .labels(eval.getMatchedLabels())
                .build();
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

    private boolean isAgriculturalLabel(Label label) {
        if (AgriculturalProduceLabel.isAgricultural(label.name())) {
            return true;
        }

        if (label.hasParents()) {
            for (Parent parent : label.parents()) {
                if (AgriculturalProduceLabel.isAgricultural(parent.name())) {
                    return true;
                }
            }
        }

        if (label.hasCategories()) {
            for (LabelCategory category : label.categories()) {
                if (AgriculturalProduceLabel.isAgricultural(category.name())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Getter
    @Builder
    private static class LabelEvaluationResult {
        float highestAgriConfidence;
        float highestNonAgriConfidence;
        String topNonAgriName;
        boolean hasHumanOrBodyPresence;
        List<ModerationCheckResult.ModerationLabelItem> matchedLabels;
        List<Map<String, Object>> rawLabelList;
    }
}
