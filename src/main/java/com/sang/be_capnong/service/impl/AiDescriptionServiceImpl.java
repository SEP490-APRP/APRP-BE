package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.dto.request.AiDescriptionSuggestionRequest;
import com.sang.be_capnong.dto.response.AiDescriptionSuggestionResponse;
import com.sang.be_capnong.enums.AgriculturalProduceLabel;
import com.sang.be_capnong.service.AiDescriptionService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class AiDescriptionServiceImpl implements AiDescriptionService {

    final ObjectMapper objectMapper;
    final HttpClient httpClient;

    @Value("${google.gemini.api-key:}")
    String apiKey;

    @Value("${google.gemini.model:gemini-1.5-flash}")
    String model;

    @Value("${google.gemini.timeout-seconds:8}")
    int timeoutSeconds;

    @Autowired
    public AiDescriptionServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public AiDescriptionSuggestionResponse suggestDescription(AiDescriptionSuggestionRequest request) {
        String productName = request.getProductName() != null ? request.getProductName().trim() : "";
        log.info("Generating AI description suggestion for product: '{}'", productName);

        // 1. Nếu có cấu hình Gemini API Key -> Thử gọi Google Gemini 1.5 Flash
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return callGeminiApi(request);
            } catch (Exception e) {
                log.warn("Gemini API call failed: {}. Falling back to smart produce template generator.", e.getMessage());
            }
        } else {
            log.info("Gemini API key not configured. Using smart produce template generator.");
        }

        // 2. Cơ chế Fallback an toàn, gọn gàng, tự động tạo mô tả chuẩn phong vị nông sản giải cứu
        return buildSmartFallback(request);
    }

    private AiDescriptionSuggestionResponse callGeminiApi(AiDescriptionSuggestionRequest request) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey.trim();

        String prompt = buildPrompt(request);

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(textPart));
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 800
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
                return parseGeneratedText(generatedText);
            }
        }

        throw new RuntimeException("Invalid response structure from Gemini API");
    }

    private String buildPrompt(AiDescriptionSuggestionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý AI chuyên nghiệp hỗ trợ nông dân đăng bán trên sàn CapNong (Sàn thương mại điện tử giải cứu nông sản Việt Nam).\n");
        sb.append("Hãy viết một bài mô tả bán hàng ngắn gọn, chân thật và truyền cảm hứng cho sản phẩm sau:\n");
        sb.append("- Tên sản phẩm: ").append(request.getProductName()).append("\n");
        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            sb.append("- Danh mục: ").append(request.getCategoryName()).append("\n");
        }
        if (request.getOrigin() != null && !request.getOrigin().isBlank()) {
            sb.append("- Xuất xứ / Vùng trồng: ").append(request.getOrigin()).append("\n");
        }

        sb.append("\nYêu cầu cấu trúc bài viết:\n");
        sb.append("1. Giọng văn chân chất của nhà nông, mộc mạc và chân thành.\n");
        sb.append("2. Nông sản giải cứu có thể có ngoại hình xấu mã, vỏ hơi rám nắng hoặc méo mó tự nhiên do chín cây ngoài vườn, hãy khéo léo biến đặc điểm đó thành minh chứng cho nông sản sạch, canh tác tự nhiên, không thuốc kích thích, chất lượng ruột bên trong ngọt ngon mọng nước.\n");
        sb.append("3. Độ dài khoảng 120 - 180 từ.\n");
        sb.append("4. Cuối bài hãy liệt kê 3 gạch đầu dòng điểm nhấn chính bắt đầu bằng ký tự '- '.\n");

        return sb.toString();
    }

    private AiDescriptionSuggestionResponse parseGeneratedText(String rawText) {
        List<String> highlights = new ArrayList<>();
        StringBuilder descBuilder = new StringBuilder();

        for (String line : rawText.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                highlights.add(trimmed.substring(2).trim());
            } else if (!trimmed.isBlank()) {
                if (descBuilder.length() > 0) {
                    descBuilder.append("\n");
                }
                descBuilder.append(trimmed);
            }
        }

        if (highlights.isEmpty()) {
            highlights = defaultHighlights();
        }

        return AiDescriptionSuggestionResponse.builder()
                .suggestedDescription(descBuilder.toString().trim())
                .keyHighlights(highlights)
                .generatedSource("GEMINI_AI")
                .build();
    }

    private AiDescriptionSuggestionResponse buildSmartFallback(AiDescriptionSuggestionRequest request) {
        String name = request.getProductName();
        String origin = (request.getOrigin() != null && !request.getOrigin().isBlank())
                ? request.getOrigin().trim()
                : "nhà vườn địa phương";

        Optional<AgriculturalProduceLabel> produceOpt = AgriculturalProduceLabel.detectExpectedProduce(name);
        String produceVi = produceOpt.map(AgriculturalProduceLabel::getVietnameseName).orElse(name);

        String description = String.format(
                "%s được bà con nông dân thu hoạch tươi mới trực tiếp tại %s. "
                + "Do canh tác theo hướng tự nhiên và chín cây ngoài ruộng, sản phẩm có thể mang hình thức mộc mạc, "
                + "vỏ ngoài hơi rám nhẹ hoặc kích thước không đồng đều, nhưng giữ trọn vị ngọt thanh thuần khiết, độ tươi giòn và dinh dưỡng tự nhiên.\n\n"
                + "CapNong đồng hành cùng nhà vườn cam kết nông sản an toàn, không sử dụng chất bảo quản độc hại, "
                + "rất thích hợp cho bữa cơm gia đình tươi ngon mỗi ngày và cùng chung tay giải cứu nông sản Việt.",
                name, origin
        );

        List<String> highlights = List.of(
                "Thu hoạch tươi mới từ " + origin + ", giữ trọn vị ngon nguyên bản",
                "Canh tác tự nhiên an toàn, không thuốc kích thích tăng trưởng",
                "Nông sản giải cứu mộc mạc: hình thức tự nhiên, ruột ngọt ngon",
                "Chung tay bảo vệ giá trị nông sản Việt và hỗ trợ thu nhập cho bà con nông dân"
        );

        return AiDescriptionSuggestionResponse.builder()
                .suggestedDescription(description)
                .keyHighlights(highlights)
                .generatedSource("SMART_TEMPLATE_FALLBACK")
                .build();
    }

    private List<String> defaultHighlights() {
        return List.of(
                "Thu hoạch tươi ngon trực tiếp từ vườn",
                "Canh tác tự nhiên, an toàn cho sức khỏe",
                "Hương vị đậm đà, giữ trọn độ tươi giòn"
        );
    }
}
