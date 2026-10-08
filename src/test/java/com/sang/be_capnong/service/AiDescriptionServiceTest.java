package com.sang.be_capnong.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.dto.request.AiDescriptionSuggestionRequest;
import com.sang.be_capnong.dto.response.AiDescriptionSuggestionResponse;
import com.sang.be_capnong.service.impl.AiDescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AiDescriptionServiceTest {

    private AiDescriptionServiceImpl aiDescriptionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Không cấu hình apiKey -> Tự động dùng Smart Fallback
        aiDescriptionService = new AiDescriptionServiceImpl(objectMapper);
    }

    @Test
    void suggestDescription_smartFallback_generatesRichVietnameseDescription() {
        AiDescriptionSuggestionRequest request = AiDescriptionSuggestionRequest.builder()
                .productName("Dưa hấu ruột đỏ Long An")
                .categoryName("Trái cây / Hoa quả")
                .origin("Long An")
                .build();

        AiDescriptionSuggestionResponse response = aiDescriptionService.suggestDescription(request);

        assertThat(response).isNotNull();
        assertThat(response.getSuggestedDescription()).contains("Dưa hấu ruột đỏ Long An");
        assertThat(response.getSuggestedDescription()).contains("Long An");
        assertThat(response.getSuggestedDescription()).contains("nông sản");
        assertThat(response.getKeyHighlights()).isNotEmpty();
        assertThat(response.getKeyHighlights().size()).isGreaterThanOrEqualTo(3);
        assertThat(response.getGeneratedSource()).isEqualTo("SMART_TEMPLATE_FALLBACK");
    }

    @Test
    void suggestDescription_localProduceWithoutOrigin_usesDefaultLocalOrigin() {
        AiDescriptionSuggestionRequest request = AiDescriptionSuggestionRequest.builder()
                .productName("Rau càng cua")
                .build();

        AiDescriptionSuggestionResponse response = aiDescriptionService.suggestDescription(request);

        assertThat(response).isNotNull();
        assertThat(response.getSuggestedDescription()).contains("Rau càng cua");
        assertThat(response.getSuggestedDescription()).contains("nhà vườn địa phương");
        assertThat(response.getKeyHighlights()).isNotEmpty();
    }
}
