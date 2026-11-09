package com.sang.be_capnong.service.impl;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.sang.be_capnong.dto.response.ImageSearchResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.enums.AgriculturalProduceLabel;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductMapper;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.repository.specification.ProductSpecification;
import com.sang.be_capnong.service.ImageSearchService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GoogleVisionImageSearchServiceImpl implements ImageSearchService {

    final ImageAnnotatorClient imageAnnotatorClient;
    final ProductRepository productRepository;
    final ProductMapper productMapper;

    // ==========================================
    // IN-MEMORY RATE LIMITING (SLIDING WINDOW LOG)
    // Tối đa 10 request / phút / Client IP
    // Không phụ thuộc Redis, bảo vệ RAM và quota API
    // ==========================================
    static final long WINDOW_MS = 60_000L;
    static final int MAX_REQUESTS_PER_MINUTE = 10;
    static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    final Map<String, ConcurrentLinkedDeque<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();

    @Autowired
    public GoogleVisionImageSearchServiceImpl(
            @Autowired(required = false) ImageAnnotatorClient imageAnnotatorClient,
            ProductRepository productRepository,
            ProductMapper productMapper
    ) {
        this.imageAnnotatorClient = imageAnnotatorClient;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Override
    public ImageSearchResponse searchProductsByImage(MultipartFile file, String clientIp) {
        // 1. Kiểm tra giới hạn tần suất gọi (Rate Limiting)
        checkRateLimit(clientIp);

        // 2. Validate tính hợp lệ của file ảnh
        validateImageFile(file);

        // 3. Nếu ImageAnnotatorClient chưa cấu hình (ví dụ môi trường test không có credentials)
        if (imageAnnotatorClient == null) {
            log.warn("ImageAnnotatorClient is null. Google Cloud Vision credentials not configured.");
            throw new AppException(ErrorCode.IMAGE_MODERATION_FAILED);
        }

        try {
            // 4. Gọi Google Cloud Vision API với LABEL_DETECTION, WEB_DETECTION và SAFE_SEARCH_DETECTION
            byte[] imageBytes = file.getBytes();
            ByteString imgBytes = ByteString.copyFrom(imageBytes);
            Image img = Image.newBuilder().setContent(imgBytes).build();

            Feature labelFeature = Feature.newBuilder()
                    .setType(Feature.Type.LABEL_DETECTION)
                    .setMaxResults(15)
                    .build();

            Feature webFeature = Feature.newBuilder()
                    .setType(Feature.Type.WEB_DETECTION)
                    .setMaxResults(15)
                    .build();

            Feature safeSearchFeature = Feature.newBuilder()
                    .setType(Feature.Type.SAFE_SEARCH_DETECTION)
                    .build();

            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .setImage(img)
                    .addFeatures(labelFeature)
                    .addFeatures(webFeature)
                    .addFeatures(safeSearchFeature)
                    .build();

            BatchAnnotateImagesResponse batchResponse = imageAnnotatorClient.batchAnnotateImages(List.of(request));
            if (batchResponse.getResponsesList().isEmpty()) {
                throw new AppException(ErrorCode.IMAGE_MODERATION_FAILED);
            }

            AnnotateImageResponse response = batchResponse.getResponses(0);
            if (response.hasError()) {
                log.error("Google Cloud Vision API returned error: {}", response.getError().getMessage());
                throw new AppException(ErrorCode.IMAGE_MODERATION_FAILED);
            }

            // 5. Phân tích kết quả nhận diện (Labels & Web Entities)
            return processVisionResponse(response);

        } catch (IOException e) {
            log.error("Error reading uploaded image bytes: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.FILE_INVALID);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Google Vision image search: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.IMAGE_MODERATION_FAILED);
        }
    }

    private ImageSearchResponse processVisionResponse(AnnotateImageResponse response) {
        // 0. Kiểm duyệt SafeSearch trước: Chặn tuyệt đối ảnh người lớn (Adult), bạo lực (Violence)
        if (response.hasSafeSearchAnnotation()) {
            SafeSearchAnnotation safeSearch = response.getSafeSearchAnnotation();
            if (isUnsafeImage(safeSearch)) {
                log.warn("Visual search rejected image due to unsafe content (SafeSearch: Adult={}, Violence={}, Racy={})",
                        safeSearch.getAdult(), safeSearch.getViolence(), safeSearch.getRacy());
                return ImageSearchResponse.builder()
                        .isAgricultural(false)
                        .identifiedProduce(null)
                        .confidence(0.0)
                        .detectedKeywords(Collections.emptyList())
                        .searchKeyword(null)
                        .products(Collections.emptyList())
                        .totalMatches(0)
                        .build();
            }
        }

        Set<String> collectedKeywords = new LinkedHashSet<>();
        AgriculturalProduceLabel matchedProduce = null;
        String bestVietnameseName = null;
        double highestConfidence = 0.0;
        boolean hasAgriculturalSignal = false;

        // A. Ưu tiên 1: Đọc từ WebDetection (bestGuessLabels & webEntities - Google Images thường có tiếng Việt tự nhiên)
        if (response.hasWebDetection()) {
            WebDetection web = response.getWebDetection();

            // Best guess labels
            for (WebDetection.WebLabel webLabel : web.getBestGuessLabelsList()) {
                String guess = webLabel.getLabel();
                if (guess != null && !guess.isBlank()) {
                    collectedKeywords.add(guess.trim());
                    Optional<AgriculturalProduceLabel> detected = AgriculturalProduceLabel.detectExpectedProduce(guess);
                    if (detected.isPresent() && detected.get().isSpecificProduce()) {
                        matchedProduce = detected.get();
                        bestVietnameseName = matchedProduce.getVietnameseName();
                        hasAgriculturalSignal = true;
                        highestConfidence = Math.max(highestConfidence, 0.95);
                        break;
                    }
                }
            }

            // Web entities
            for (WebDetection.WebEntity entity : web.getWebEntitiesList()) {
                String desc = entity.getDescription();
                if (desc != null && !desc.isBlank()) {
                    collectedKeywords.add(desc.trim());
                    
                    Optional<AgriculturalProduceLabel> detected = AgriculturalProduceLabel.detectExpectedProduce(desc);
                    if (detected.isPresent()) {
                        hasAgriculturalSignal = true;
                        if (matchedProduce == null && detected.get().isSpecificProduce()) {
                            matchedProduce = detected.get();
                            bestVietnameseName = matchedProduce.getVietnameseName();
                            highestConfidence = Math.max(highestConfidence, entity.getScore());
                        }
                    } else {
                        Optional<AgriculturalProduceLabel> fromLabel = AgriculturalProduceLabel.findByLabel(desc);
                        if (fromLabel.isPresent()) {
                            hasAgriculturalSignal = true;
                            if (matchedProduce == null && fromLabel.get().isSpecificProduce()) {
                                matchedProduce = fromLabel.get();
                                bestVietnameseName = matchedProduce.getVietnameseName();
                                highestConfidence = Math.max(highestConfidence, entity.getScore());
                            }
                        }
                    }
                }
            }
        }

        // B. Ưu tiên 2: Đọc từ LabelAnnotations
        for (EntityAnnotation label : response.getLabelAnnotationsList()) {
            String desc = label.getDescription();
            if (desc == null || desc.isBlank()) continue;

            collectedKeywords.add(desc.trim());

            // Kiểm tra nhãn nông sản
            if (AgriculturalProduceLabel.isAgricultural(desc)) {
                hasAgriculturalSignal = true;
                highestConfidence = Math.max(highestConfidence, label.getScore());

                if (matchedProduce == null) {
                    Optional<AgriculturalProduceLabel> opt = AgriculturalProduceLabel.findByLabel(desc);
                    if (opt.isPresent() && opt.get().isSpecificProduce()) {
                        matchedProduce = opt.get();
                        bestVietnameseName = matchedProduce.getVietnameseName();
                    } else {
                        Optional<AgriculturalProduceLabel> fromDetect = AgriculturalProduceLabel.detectExpectedProduce(desc);
                        if (fromDetect.isPresent() && fromDetect.get().isSpecificProduce()) {
                            matchedProduce = fromDetect.get();
                            bestVietnameseName = matchedProduce.getVietnameseName();
                        }
                    }
                }
            }
        }

        // C. Nếu không có bất kỳ tín hiệu nông sản nào -> Ảnh đồ vật/người/không liên quan
        if (!hasAgriculturalSignal) {
            log.info("Uploaded image contains no agricultural produce signals. Rejecting as non-agricultural.");
            return ImageSearchResponse.builder()
                    .isAgricultural(false)
                    .identifiedProduce(null)
                    .confidence(0.0)
                    .detectedKeywords(new ArrayList<>(collectedKeywords))
                    .searchKeyword(null)
                    .products(Collections.emptyList())
                    .totalMatches(0)
                    .build();
        }

        // D. Xác định từ khóa chính (Search Keyword) và từ khóa loại trừ (Exclude Keyword)
        String searchKeyword;
        String excludeKeyword = null;

        if (matchedProduce != null) {
            searchKeyword = matchedProduce.getVietnameseName();
            excludeKeyword = matchedProduce.getExcludeKeyword();
            if (bestVietnameseName == null) {
                bestVietnameseName = matchedProduce.getVietnameseName();
            }
        } else {
            // Nông sản lạ hoặc ngoài Enum: Trích xuất tên tự nhiên từ Google Vision (BestGuessLabel hoặc WebEntity)
            String naturalName = extractBestNaturalProduceName(response);
            if (naturalName != null && !naturalName.isBlank()) {
                bestVietnameseName = naturalName;
                searchKeyword = naturalName;
            } else {
                // Nhận diện theo nhóm thực vật / nông sản chung (FRUIT, VEGETABLE, ROOT VEGETABLE)
                if (hasSignal(response, "FRUIT")) {
                    bestVietnameseName = "Trái cây tươi";
                    searchKeyword = "Trái cây";
                } else if (hasSignal(response, "ROOT VEGETABLE") || hasSignal(response, "TUBER")) {
                    bestVietnameseName = "Củ tươi";
                    searchKeyword = "Củ";
                } else if (hasSignal(response, "LEAF VEGETABLE") || hasSignal(response, "VEGETABLE")) {
                    bestVietnameseName = "Rau xanh tươi";
                    searchKeyword = "Rau";
                } else {
                    bestVietnameseName = "Nông sản tươi";
                    searchKeyword = ""; // Để trống để query lấy nông sản hoạt động, không lọc từ khóa rác "tươi"
                }
            }
        }

        // E. Truy vấn kho hàng nông sản đang bán khớp với từ khóa qua Specification
        Specification<Product> spec = ProductSpecification.searchForVisualSearch(searchKeyword, excludeKeyword);
        Page<Product> productPage = productRepository.findAll(
                spec,
                PageRequest.of(0, 12, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        List<Product> matchedProducts = productPage.getContent();

        List<ProductResponse> productResponses = productMapper.toProductResponseList(matchedProducts);

        log.info("Visual search recognized '{}' (keyword: '{}', exclude: '{}') with {} active products found.",
                bestVietnameseName, searchKeyword, excludeKeyword, productResponses.size());

        return ImageSearchResponse.builder()
                .isAgricultural(true)
                .identifiedProduce(bestVietnameseName)
                .confidence(Math.round(highestConfidence * 100.0) / 100.0)
                .detectedKeywords(new ArrayList<>(collectedKeywords))
                .searchKeyword(searchKeyword)
                .products(productResponses)
                .totalMatches(productResponses.size())
                .build();
    }

    private static final Set<String> GENERIC_PRODUCE_TERMS = Set.of(
            "fruit", "vegetable", "produce", "food", "plant", "natural foods",
            "whole food", "local food", "superfood", "vegetarian food", "ingredient",
            "staple food", "crop", "agriculture", "flora", "harvest", "herb", "seed",
            "root vegetable", "leaf vegetable", "citrus", "berry", "legume", "gourd",
            "squash", "grain", "tuber", "fungus"
    );

    private boolean isUnsafeImage(SafeSearchAnnotation safeSearch) {
        if (safeSearch == null) return false;
        Likelihood adult = safeSearch.getAdult();
        Likelihood violence = safeSearch.getViolence();
        Likelihood racy = safeSearch.getRacy();

        boolean isAdult = adult == Likelihood.LIKELY || adult == Likelihood.VERY_LIKELY;
        boolean isViolence = violence == Likelihood.LIKELY || violence == Likelihood.VERY_LIKELY;
        boolean isRacy = racy == Likelihood.VERY_LIKELY;

        return isAdult || isViolence || isRacy;
    }

    private String extractBestNaturalProduceName(AnnotateImageResponse response) {
        if (response.hasWebDetection()) {
            WebDetection web = response.getWebDetection();
            for (WebDetection.WebLabel webLabel : web.getBestGuessLabelsList()) {
                String label = webLabel.getLabel();
                if (label != null && !label.isBlank() && !GENERIC_PRODUCE_TERMS.contains(label.trim().toLowerCase(Locale.ROOT))) {
                    return label.trim();
                }
            }

            for (WebDetection.WebEntity entity : web.getWebEntitiesList()) {
                String desc = entity.getDescription();
                if (desc != null && !desc.isBlank() && entity.getScore() >= 0.5f) {
                    if (!GENERIC_PRODUCE_TERMS.contains(desc.trim().toLowerCase(Locale.ROOT))) {
                        return desc.trim();
                    }
                }
            }
        }

        for (EntityAnnotation label : response.getLabelAnnotationsList()) {
            String desc = label.getDescription();
            if (desc != null && !desc.isBlank() && !GENERIC_PRODUCE_TERMS.contains(desc.trim().toLowerCase(Locale.ROOT))) {
                return desc.trim();
            }
        }

        return null;
    }

    private boolean hasSignal(AnnotateImageResponse response, String labelName) {
        for (EntityAnnotation label : response.getLabelAnnotationsList()) {
            if (label.getDescription() != null && label.getDescription().equalsIgnoreCase(labelName)) {
                return true;
            }
        }
        return false;
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_INVALID);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Image file size {} exceeds limit of 5MB", file.getSize());
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !(
                contentType.equalsIgnoreCase("image/jpeg") ||
                contentType.equalsIgnoreCase("image/png") ||
                contentType.equalsIgnoreCase("image/webp") ||
                contentType.equalsIgnoreCase("image/jpg")
        )) {
            log.warn("Unsupported content type for visual search: {}", contentType);
            throw new AppException(ErrorCode.FILE_INVALID);
        }
    }

    private void checkRateLimit(String clientIp) {
        String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "ANONYMOUS";
        long now = System.currentTimeMillis();

        ConcurrentLinkedDeque<Long> timestamps = ipRequestTimestamps.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Loại bỏ các request đã cũ hơn 60 giây (Sliding Window)
        while (!timestamps.isEmpty() && timestamps.peekFirst() < now - WINDOW_MS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Visual search rate limit exceeded for IP {}: {} requests in 1 minute", key, timestamps.size());
            throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }

        timestamps.addLast(now);
    }

    /**
     * Dọn dẹp các IP không còn hoạt động mỗi 5 phút để bảo vệ bộ nhớ RAM
     */
    @Scheduled(fixedRate = 300_000)
    public void cleanupExpiredRateLimitEntries() {
        long now = System.currentTimeMillis();
        ipRequestTimestamps.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Long> deque = entry.getValue();
            while (!deque.isEmpty() && deque.peekFirst() < now - WINDOW_MS) {
                deque.pollFirst();
            }
            return deque.isEmpty();
        });
    }

    public void resetRateLimitForTesting() {
        ipRequestTimestamps.clear();
    }
}

