package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.dto.request.AiDescriptionSuggestionRequest;
import com.sang.be_capnong.dto.response.AiDescriptionSuggestionResponse;
import com.sang.be_capnong.enums.AgriculturalProduceLabel;
import com.sang.be_capnong.enums.ProhibitedContentCategory;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.AiDescriptionService;
import com.sang.be_capnong.util.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AiDescriptionServiceImpl implements AiDescriptionService {

    final ObjectMapper objectMapper;
    final HttpClient httpClient;
    final Environment environment;
    final List<String> activeApiKeys = new CopyOnWriteArrayList<>();
    final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    @Autowired(required = false)
    UserRepository userRepository;

    // Rate Limiting In-Memory (10 requests / 1 minute / user)
    final Map<Integer, ConcurrentLinkedDeque<Long>> userRequestTimestamps = new ConcurrentHashMap<>();
    static final int MAX_REQUESTS_PER_MINUTE = 10;
    static final long WINDOW_MS = 60_000L;

    @Value("${google.gemini.api-key:}")
    String defaultApiKey;

    @Value("${google.gemini.model:gemini-2.5-flash}")
    String model = "gemini-2.5-flash";

    @Value("${google.gemini.timeout-seconds:8}")
    int timeoutSeconds = 8;

    @Autowired
    public AiDescriptionServiceImpl(ObjectMapper objectMapper, @Autowired(required = false) Environment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public AiDescriptionServiceImpl(ObjectMapper objectMapper) {
        this(objectMapper, (Environment) null);
    }

    public void setActiveApiKeysForTesting(List<String> testApiKeys) {
        this.activeApiKeys.clear();
        if (testApiKeys != null) {
            this.activeApiKeys.addAll(testApiKeys);
        }
    }

    public void resetRateLimitForTesting() {
        this.userRequestTimestamps.clear();
    }

    @PostConstruct
    public void initApiKeys() {
        Set<String> keySet = new LinkedHashSet<>();

        // 1. Quét key từ cấu hình google.gemini.api-key (hỗ trợ phân tách bằng dấu phẩy)
        if (defaultApiKey != null && !defaultApiKey.isBlank()) {
            for (String key : defaultApiKey.split(",")) {
                String trimmed = key.trim();
                if (!trimmed.isEmpty()) {
                    keySet.add(trimmed);
                }
            }
        }

        // 2. Quét biến môi trường GEMINI_API_KEY
        if (environment != null) {
            String envKey = environment.getProperty("GEMINI_API_KEY");
            if (envKey != null && !envKey.isBlank()) {
                keySet.add(envKey.trim());
            }

            // 3. Quét danh sách GEMINI_KEY_1 -> GEMINI_KEY_50 (cho phép xoay vòng key)
            for (int i = 1; i <= 50; i++) {
                String rotatedKey = environment.getProperty("GEMINI_KEY_" + i);
                if (rotatedKey != null && !rotatedKey.isBlank()) {
                    keySet.add(rotatedKey.trim());
                }
            }
        }

        activeApiKeys.clear();
        activeApiKeys.addAll(keySet);
        if (!activeApiKeys.isEmpty()) {
            log.info("Initialized Gemini AI Gateway with {} active API keys for auto-rotation", activeApiKeys.size());
        }
    }

    @Override
    public AiDescriptionSuggestionResponse suggestDescription(AiDescriptionSuggestionRequest request) {
        if (request == null || request.getProductName() == null || request.getProductName().trim().isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_REQUIRED);
        }

        String productName = request.getProductName().trim();
        if (productName.length() > 255) {
            throw new AppException(ErrorCode.PRODUCT_NAME_TOO_LONG);
        }

        String productType = request.getProductType() != null ? request.getProductType().trim().toUpperCase() : "SINGLE";
        log.info("Generating AI description suggestion for product: '{}' (type: {})", productName, productType);

        // 0. Kiểm tra tài khoản Shop hợp lệ (không bị khóa hoặc chưa kích hoạt)
        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId != null && userRepository != null) {
            User shopUser = userRepository.findById(currentUserId).orElse(null);
            if (shopUser != null) {
                if (shopUser.getStatus() == UserStatus.LOCKED) {
                    throw new AppException(ErrorCode.USER_LOCKED);
                }
                if (shopUser.getStatus() == UserStatus.PENDING) {
                    throw new AppException(ErrorCode.USER_PENDING_APPROVAL);
                }
                if (shopUser.getStatus() != UserStatus.ACTIVE || Boolean.FALSE.equals(shopUser.getShopActive())) {
                    throw new AppException(ErrorCode.SHOP_NOT_FOUND);
                }
            }
        }

        // 1. Tiền kiểm duyệt an toàn nội dung toàn diện (Content Safety & Prompt Injection across all fields)
        validateContentSafety(request, productName);

        // 2. Kiểm tra giới hạn tần suất gọi API (Rate Limiting)
        checkRateLimit();

        // 3. Nếu có cấu hình danh sách Gemini API Keys -> Thử gọi xoay vòng qua các key
        if (!activeApiKeys.isEmpty()) {
            int totalKeys = activeApiKeys.size();
            int attempts = 0;

            while (attempts < totalKeys) {
                int index = Math.abs(currentKeyIndex.get()) % totalKeys;
                String currentKey = activeApiKeys.get(index);

                try {
                    return callGeminiApi(request, currentKey, productType);
                } catch (Exception e) {
                    attempts++;
                    log.warn("Gemini API call failed with key index {}/{}: {}. Rotating to next key...",
                            index + 1, totalKeys, e.getMessage());
                    // Xoay sang key tiếp theo
                    currentKeyIndex.incrementAndGet();
                }
            }

            log.warn("All {} Gemini API keys exhausted or failed. Falling back to smart produce template generator.", totalKeys);
        } else {
            log.info("No Gemini API keys configured. Using smart produce template generator.");
        }

        // 4. Cơ chế Fallback an toàn đa ngữ cảnh (Single, Mystery Box, Combo)
        return buildSmartFallback(request, productType);
    }

    private void validateContentSafety(AiDescriptionSuggestionRequest request, String productName) {
        StringBuilder sb = new StringBuilder();
        sb.append(productName).append(" ");
        if (request.getOrigin() != null) sb.append(request.getOrigin()).append(" ");
        if (request.getCategoryName() != null) sb.append(request.getCategoryName()).append(" ");
        if (request.getCurrentDescription() != null) sb.append(request.getCurrentDescription()).append(" ");
        if (request.getIncludedItems() != null) {
            for (String item : request.getIncludedItems()) {
                if (item != null) sb.append(item).append(" ");
            }
        }

        String combined = sb.toString().trim();
        Optional<ProhibitedContentCategory> violation = ProhibitedContentCategory.findViolation(combined);
        if (violation.isPresent()) {
            ProhibitedContentCategory category = violation.get();
            log.warn("Prohibited content detected in AI description request: [{}] - {}", category.name(), category.getDescription());
            throw new AppException(ErrorCode.AI_INAPPROPRIATE_CONTENT);
        }
    }

    private void checkRateLimit() {
        Integer userId = SecurityUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> timestamps = userRequestTimestamps.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());

        while (!timestamps.isEmpty() && timestamps.peekFirst() < now - WINDOW_MS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for user ID {}: {} requests in 1 minute", userId, timestamps.size());
            throw new AppException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }

        timestamps.addLast(now);
    }

    private AiDescriptionSuggestionResponse callGeminiApi(AiDescriptionSuggestionRequest request, String apiKey, String productType) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

        String prompt = buildPrompt(request, productType);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 600
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API returned status code " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (!textNode.isMissingNode()) {
                String generatedText = textNode.asText().trim();
                return parseGeneratedText(generatedText, productType, request.getMealType());
            }
        }

        throw new RuntimeException("Invalid response structure from Gemini API");
    }

    private String buildPrompt(AiDescriptionSuggestionRequest request, String productType) {
        StringBuilder sb = new StringBuilder();

        if ("MYSTERY_BOX".equalsIgnoreCase(productType)) {
            // Nhánh Túi Mù (Mystery Box)
            sb.append("Bạn là chuyên gia sáng tạo nội dung cho sàn CapNong (Sàn thương mại điện tử giải cứu nông sản Việt Nam).\n");
            sb.append("Nhiệm vụ: Viết bài mô tả hấp dẫn cho gói TÚI MÙ NÔNG SẢN (Mystery Box / Hộp quà nông sản bí ẩn):\n");
            sb.append("- Tên gói túi mù: ").append(request.getProductName()).append("\n");
            if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
                sb.append("- Loại túi mù: ").append(request.getCategoryName()).append("\n");
            }
            if (request.getIncludedItems() != null && !request.getIncludedItems().isEmpty()) {
                sb.append("- Các nông sản có thể xuất hiện trong hộp: ").append(String.join(", ", request.getIncludedItems())).append("\n");
            }
            if (request.getOrigin() != null && !request.getOrigin().isBlank()) {
                sb.append("- Vùng thu hoạch: ").append(request.getOrigin()).append("\n");
            }

            sb.append("\nQUY TẮC BẮT BUỘC:\n");
            sb.append("1. Phong cách cuốn hút, tạo cảm giác tò mò và háo hức khi khui hộp bất ngờ.\n");
            sb.append("2. Nhấn mạnh ý nghĩa đồng hành cùng nhà vườn giải cứu nông sản theo mùa, cam kết đủ khối lượng, sản phẩm tươi sạch mộc mạc và an toàn.\n");
            sb.append("3. TUYỆT ĐỐI KHÔNG hứa hẹn các loại trái cây đắt tiền cụ thể nếu không có trong danh sách nông sản tiềm năng.\n");
            sb.append("4. GIỚI HẠN ĐỘ DÀI: Tối đa 380 ký tự (khoảng 50 - 70 từ) để tương thích hiển thị trên ứng dụng.\n");
            sb.append("5. Cuối bài hãy liệt kê 3 điểm nổi bật bắt đầu bằng dấu gạch đầu dòng '- '.\n");

        } else if ("COMBO".equalsIgnoreCase(productType)) {
            // Nhánh Combo (Build Combo / Mâm cơm gia đình)
            sb.append("Bạn là chuyên gia ẩm thực và sáng tạo nội dung cho sàn CapNong.\n");
            sb.append("Nhiệm vụ: Viết mô tả kích thích vị giác và tiện lợi cho COMBO NÔNG SẢN (Set nguyên liệu nấu ăn / Mâm cơm):\n");
            sb.append("- Tên Combo: ").append(request.getProductName()).append("\n");
            if (request.getMealType() != null && !request.getMealType().isBlank()) {
                sb.append("- Kiểu bữa ăn (MealType): ").append(request.getMealType()).append("\n");
            }
            if (request.getIncludedItems() != null && !request.getIncludedItems().isEmpty()) {
                sb.append("- Các nguyên liệu trong combo: ").append(String.join(", ", request.getIncludedItems())).append("\n");
            }
            if (request.getOrigin() != null && !request.getOrigin().isBlank()) {
                sb.append("- Vùng nguyên liệu: ").append(request.getOrigin()).append("\n");
            }

            sb.append("\nQUY TẮC BẮT BUỘC:\n");
            sb.append("1. Nhấn mạnh tính tiện lợi: Đi chợ 1 lần đủ món, nguyên liệu tươi ngon được chuẩn bị sẵn, tiết kiệm thời gian chuẩn bị bữa cơm gia đình.\n");
            if (request.getMealType() != null && request.getMealType().toUpperCase().contains("VEGETARIAN")) {
                sb.append("2. LƯU Ý ĐẶC BIỆT: Đây là combo ĂN CHAY (VEGETARIAN). TUYỆT ĐỐI KHÔNG đề cập đến thịt, cá, hải sản hay nguyên liệu mặn. Tập trung vào vị ngọt thanh tự nhiên từ rau củ quả, nấm tươi thanh tịnh.\n");
            } else {
                sb.append("2. Gợi ý bữa ăn ấm cúng, tròn vị tươi mát của rau củ nhà vườn.\n");
            }
            sb.append("3. GIỚI HẠN ĐỘ DÀI: Tối đa 380 ký tự (khoảng 50 - 70 từ) để tương thích hiển thị trên ứng dụng.\n");
            sb.append("4. Cuối bài hãy liệt kê 3 điểm nổi bật bắt đầu bằng dấu gạch đầu dòng '- '.\n");

        } else {
            // Nhánh Nông Sản Đơn Lẻ (Single Produce)
            sb.append("Bạn là trợ lý AI chuyên nghiệp hỗ trợ nông dân đăng bán trên sàn CapNong (Sàn thương mại điện tử giải cứu nông sản Việt Nam).\n");
            sb.append("Hãy viết một bài mô tả bán hàng ngắn gọn, chân thật và truyền cảm hứng cho nông sản sau:\n");
            sb.append("- Tên sản phẩm: ").append(request.getProductName()).append("\n");
            if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
                sb.append("- Danh mục: ").append(request.getCategoryName()).append("\n");
            }
            if (request.getOrigin() != null && !request.getOrigin().isBlank()) {
                sb.append("- Xuất xứ / Vùng trồng: ").append(request.getOrigin()).append("\n");
            }

            sb.append("\nQUY TẮC BẮT BUỘC:\n");
            sb.append("1. Giọng văn chân chất của nhà nông, mộc mạc và chân thành.\n");
            sb.append("2. Nông sản giải cứu có thể có ngoại hình xấu mã, vỏ hơi rám nắng hoặc méo mó tự nhiên do chín cây ngoài vườn, hãy khéo léo biến đặc điểm đó thành minh chứng cho nông sản an toàn, không thuốc kích thích, chất lượng ruột bên trong tươi ngon, mọng nước.\n");
            sb.append("3. TUYỆT ĐỐI KHÔNG tự bịa đặt chứng chỉ hữu cơ quốc tế hoặc cam kết y tế/chữa bệnh nếu không có thông tin.\n");
            sb.append("4. Độ dài khoảng 100 - 150 từ.\n");
            sb.append("5. Cuối bài hãy liệt kê 3 điểm nổi bật chính bắt đầu bằng ký tự '- '.\n");
        }

        return sb.toString();
    }

    public AiDescriptionSuggestionResponse parseGeneratedText(String rawText, String productType, String mealType) {
        List<String> highlights = new ArrayList<>();
        StringBuilder descBuilder = new StringBuilder();

        for (String line : rawText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            // Lọc các tiêu đề Markdown thừa
            String lower = trimmed.toLowerCase();
            if (lower.startsWith("**điểm nổi bật") || lower.startsWith("*điểm nổi bật")
                    || lower.startsWith("### điểm nổi bật") || lower.startsWith("## điểm nổi bật")
                    || lower.startsWith("**mô tả") || lower.startsWith("### mô tả")
                    || lower.startsWith("## mô tả") || lower.startsWith("**giới thiệu")) {
                continue;
            }

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                highlights.add(trimmed.substring(2).trim());
            } else if (trimmed.matches("^\\d+[\\.\\)]\\s+.*")) {
                highlights.add(trimmed.replaceFirst("^\\d+[\\.\\)]\\s+", "").trim());
            } else {
                if (descBuilder.length() > 0) {
                    descBuilder.append("\n");
                }
                descBuilder.append(trimmed);
            }
        }

        if (highlights.isEmpty()) {
            highlights = defaultHighlights(productType, mealType);
        }

        String desc = descBuilder.toString().trim();
        // Kiểm soát độ dài nghiêm ngặt cho Mystery Box & Combo (không bao giờ vượt quá 420 ký tự để an toàn với @Size(max=500))
        if (("MYSTERY_BOX".equalsIgnoreCase(productType) || "COMBO".equalsIgnoreCase(productType)) && desc.length() > 420) {
            desc = desc.substring(0, 417) + "...";
        }

        return AiDescriptionSuggestionResponse.builder()
                .suggestedDescription(desc)
                .keyHighlights(highlights)
                .generatedSource("GEMINI_AI")
                .build();
    }

    private AiDescriptionSuggestionResponse buildSmartFallback(AiDescriptionSuggestionRequest request, String productType) {
        String name = request.getProductName();
        String origin = (request.getOrigin() != null && !request.getOrigin().isBlank())
                ? request.getOrigin().trim()
                : "nhà vườn địa phương";

        String description;
        List<String> highlights;

        if ("MYSTERY_BOX".equalsIgnoreCase(productType)) {
            String itemsNote = (request.getIncludedItems() != null && !request.getIncludedItems().isEmpty())
                    ? String.format("Hộp gồm các loại nông sản tuyển chọn theo mùa như %s, ", String.join(", ", request.getIncludedItems()))
                    : "Hộp gồm ngẫu nhiên các loại rau củ quả chín cây theo mùa, ";
            description = String.format(
                    "%s mang đến trải nghiệm khui quà bất ngờ đầy thú vị từ nhà vườn %s. %s"
                    + "được bà con thu hoạch tươi mới mỗi ngày. Dù mang vẻ ngoài mộc mạc, mỗi nông sản đều giữ trọn độ tươi giòn, "
                    + "an toàn và chuẩn vị ngọt lành, cùng bạn chung tay ủng hộ nông sản Việt.",
                    name, origin, itemsNote
            );
            if (description.length() > 420) {
                description = description.substring(0, 417) + "...";
            }
            highlights = List.of(
                    "Trải nghiệm khui hộp bất ngờ với nông sản tươi mới theo mùa",
                    "Cam kết đủ khối lượng, canh tác an toàn không thuốc kích thích",
                    "Đồng hành cùng nhà vườn địa phương giải cứu nông sản Việt"
            );

        } else if ("COMBO".equalsIgnoreCase(productType)) {
            boolean isVeg = request.getMealType() != null && request.getMealType().toUpperCase().contains("VEGETARIAN");
            String vibe = isVeg ? "bữa cơm chay thanh đạm, thanh tịnh" : "bữa ăn gia đình ấm cúng, trọn vị";
            String ingredients = (request.getIncludedItems() != null && !request.getIncludedItems().isEmpty())
                    ? " gồm " + String.join(", ", request.getIncludedItems())
                    : "";
            description = String.format(
                    "%s%s được phối hợp tiện lợi cho %s. "
                    + "Tất cả nguyên liệu từ vùng trồng %s đều được tuyển chọn tươi mới mỗi sớm, "
                    + "giúp bạn tiết kiệm thời gian đi chợ mà vẫn chuẩn bị được mâm cơm lành mạnh, tràn đầy dinh dưỡng tự nhiên.",
                    name, ingredients, vibe, origin
            );
            if (description.length() > 420) {
                description = description.substring(0, 417) + "...";
            }
            highlights = List.of(
                    "Tiện lợi tối đa: Đi chợ một lần đủ nguyên liệu tươi ngon",
                    "Tuyển chọn tươi mới từ nhà vườn, giữ trọn dinh dưỡng tự nhiên",
                    isVeg ? "Thanh đạm thuần chay, an lành cho sức khỏe cả nhà" : "Phối hợp hoàn hảo cho bữa cơm gia đình đầm ấm"
            );

        } else {
            Optional<AgriculturalProduceLabel> produceOpt = AgriculturalProduceLabel.detectExpectedProduce(name);
            if (produceOpt.isPresent()) {
                description = String.format(
                        "%s được bà con nông dân thu hoạch tươi mới trực tiếp tại %s. "
                        + "Do canh tác theo hướng tự nhiên và chín cây ngoài ruộng, sản phẩm có thể mang hình thức mộc mạc, "
                        + "vỏ ngoài hơi rám nhẹ hoặc kích thước không đồng đều, nhưng giữ trọn vị ngọt thanh thuần khiết, độ tươi giòn và dinh dưỡng tự nhiên.\n\n"
                        + "CapNong đồng hành cùng nhà vườn cam kết nông sản an toàn, không sử dụng chất bảo quản độc hại, "
                        + "rất thích hợp cho bữa cơm gia đình tươi ngon mỗi ngày và cùng chung tay giải cứu nông sản Việt.",
                        name, origin
                );
            } else {
                description = String.format(
                        "%s được cung cấp bởi nhà vườn uy tín tại %s. "
                        + "Sản phẩm được tuyển chọn kỹ lưỡng, đảm bảo tiêu chuẩn chất lượng và an toàn thực phẩm khi đến tay người tiêu dùng.\n\n"
                        + "CapNong cam kết minh bạch nguồn gốc xuất xứ, đồng hành cùng bà con nhà vườn mang đến trải nghiệm mua sắm tin cậy và tiện lợi.",
                        name, origin
                );
            }
            highlights = List.of(
                    "Thu hoạch tươi mới từ " + origin + ", giữ trọn vị ngon nguyên bản",
                    "Canh tác tự nhiên an toàn, không thuốc kích thích tăng trưởng",
                    "Nông sản giải cứu mộc mạc: hình thức tự nhiên, ruột ngọt ngon",
                    "Chung tay bảo vệ giá trị nông sản Việt và hỗ trợ thu nhập cho bà con nông dân"
            );
        }

        return AiDescriptionSuggestionResponse.builder()
                .suggestedDescription(description)
                .keyHighlights(highlights)
                .generatedSource("SMART_TEMPLATE_FALLBACK")
                .build();
    }

    private List<String> defaultHighlights(String productType, String mealType) {
        if ("MYSTERY_BOX".equalsIgnoreCase(productType)) {
            return List.of(
                    "Trải nghiệm khui hộp bất ngờ với nông sản tươi mới theo mùa",
                    "Cam kết đủ khối lượng, an toàn cho bữa ăn gia đình",
                    "Đồng hành cùng nhà vườn địa phương giải cứu nông sản Việt"
            );
        }
        if ("COMBO".equalsIgnoreCase(productType)) {
            boolean isVeg = mealType != null && mealType.toUpperCase().contains("VEGETARIAN");
            return List.of(
                    "Tiện lợi tối đa: Đi chợ một lần đủ nguyên liệu tươi ngon",
                    "Tuyển chọn tươi mới từ nhà vườn, giữ trọn dinh dưỡng tự nhiên",
                    isVeg ? "Thanh đạm thuần chay, an lành cho sức khỏe cả nhà" : "Phối hợp hoàn hảo cho bữa cơm gia đình đầm ấm"
            );
        }
        return List.of(
                "Thu hoạch chín cây trực tiếp từ nhà vườn, tươi ngon mộc mạc",
                "Canh tác tự nhiên an toàn, không thuốc kích thích tăng trưởng",
                "Hương vị đậm đà, trọn vẹn vị ngon nguyên bản của nông sản Việt"
        );
    }
}
