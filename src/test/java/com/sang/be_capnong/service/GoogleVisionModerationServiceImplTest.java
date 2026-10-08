package com.sang.be_capnong.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vision.v1.*;
import com.sang.be_capnong.dto.ModerationCheckResult;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.impl.GoogleVisionModerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleVisionModerationServiceImplTest {

    @Mock
    ImageAnnotatorClient imageAnnotatorClient;

    @Mock
    ImageDownloadService imageDownloadService;

    ObjectMapper objectMapper = new ObjectMapper();

    GoogleVisionModerationServiceImpl moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new GoogleVisionModerationServiceImpl(
                imageAnnotatorClient, objectMapper, imageDownloadService);
        ReflectionTestUtils.setField(moderationService, "minConfidenceThreshold", 70.0f);
        ReflectionTestUtils.setField(moderationService, "nonAgriThreshold", 80.0f);
        ReflectionTestUtils.setField(moderationService, "maxLabels", 30);
    }

    private AnnotateImageResponse buildAnnotateResponse(
            List<EntityAnnotation> labels,
            SafeSearchAnnotation safeSearch
    ) {
        AnnotateImageResponse.Builder builder = AnnotateImageResponse.newBuilder();
        if (labels != null) {
            builder.addAllLabelAnnotations(labels);
        }
        if (safeSearch != null) {
            builder.setSafeSearchAnnotation(safeSearch);
        }
        return builder.build();
    }

    @Test
    @DisplayName("Nông sản hợp lệ (Cà chua, 95%) -> Chấp thuận isAgricultural=true")
    void checkImageUrl_validProduce_acceptsImage() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        EntityAnnotation tomato = EntityAnnotation.newBuilder()
                .setDescription("Tomato")
                .setScore(0.95f)
                .build();
        EntityAnnotation vegetable = EntityAnnotation.newBuilder()
                .setDescription("Vegetable")
                .setScore(0.92f)
                .build();

        SafeSearchAnnotation safeSearch = SafeSearchAnnotation.newBuilder()
                .setAdult(Likelihood.VERY_UNLIKELY)
                .setViolence(Likelihood.VERY_UNLIKELY)
                .setRacy(Likelihood.VERY_UNLIKELY)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(tomato, vegetable), safeSearch))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/tomato.jpg");

        assertThat(result.isAgricultural()).isTrue();
        assertThat(result.isFlagged()).isFalse();
        assertThat(result.getRejectionReason()).isNull();
        assertThat(result.getOverallConfidence()).isNotNull();
        assertThat(result.getLabels()).isNotEmpty();
    }

    @Test
    @DisplayName("Đồ vật vô tri (Ô tô, Xe cộ) -> Từ chối isAgricultural=false")
    void checkImageUrl_inanimateNonAgriObject_rejectsImage() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        EntityAnnotation car = EntityAnnotation.newBuilder()
                .setDescription("Car")
                .setScore(0.98f)
                .build();
        EntityAnnotation wheel = EntityAnnotation.newBuilder()
                .setDescription("Wheel")
                .setScore(0.90f)
                .build();

        SafeSearchAnnotation safeSearch = SafeSearchAnnotation.newBuilder()
                .setAdult(Likelihood.VERY_UNLIKELY)
                .setViolence(Likelihood.VERY_UNLIKELY)
                .setRacy(Likelihood.VERY_UNLIKELY)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(car, wheel), safeSearch))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/car.jpg");

        assertThat(result.isAgricultural()).isFalse();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getRejectionReason()).contains("không phải là nông sản");
    }

    @Test
    @DisplayName("Thức ăn nhanh (Hamburger, Pizza) -> Từ chối thực phẩm chế biến sẵn")
    void checkImageUrl_fastFood_rejectsProcessedFood() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        EntityAnnotation burger = EntityAnnotation.newBuilder()
                .setDescription("Hamburger")
                .setScore(0.92f)
                .build();

        SafeSearchAnnotation safeSearch = SafeSearchAnnotation.newBuilder()
                .setAdult(Likelihood.VERY_UNLIKELY)
                .setViolence(Likelihood.VERY_UNLIKELY)
                .setRacy(Likelihood.VERY_UNLIKELY)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(burger), safeSearch))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/burger.jpg");

        assertThat(result.isAgricultural()).isFalse();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getRejectionReason()).contains("thực phẩm chế biến sẵn");
    }

    @Test
    @DisplayName("Nội dung nhạy cảm người lớn (SafeSearch Adult = LIKELY) -> Từ chối")
    void checkImageUrl_adultContent_rejectsUnsafe() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        SafeSearchAnnotation unsafeSearch = SafeSearchAnnotation.newBuilder()
                .setAdult(Likelihood.LIKELY)
                .setViolence(Likelihood.VERY_UNLIKELY)
                .setRacy(Likelihood.VERY_UNLIKELY)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(), unsafeSearch))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/nsfw.jpg");

        assertThat(result.isAgricultural()).isFalse();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getRejectionReason()).contains("SafeSearch: Adult");
    }

    @Test
    @DisplayName("Chống oan sai: Quả chuối/cà tím (Agri >= 85%) và Racy LIKELY -> Bỏ qua false positive")
    void checkImageUrl_falsePositiveBanana_bypassesMildRacy() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        EntityAnnotation banana = EntityAnnotation.newBuilder()
                .setDescription("Banana")
                .setScore(0.95f)
                .build();

        SafeSearchAnnotation mildRacy = SafeSearchAnnotation.newBuilder()
                .setAdult(Likelihood.VERY_UNLIKELY)
                .setViolence(Likelihood.VERY_UNLIKELY)
                .setRacy(Likelihood.LIKELY)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(banana), mildRacy))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/banana.jpg");

        assertThat(result.isAgricultural()).isTrue();
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    @DisplayName("Client null (chưa cấu hình credentials) -> Kích hoạt Controlled Fail-Open")
    void checkImageUrl_nullClient_activatesFailOpen() {
        GoogleVisionModerationServiceImpl unconfiguredService =
                new GoogleVisionModerationServiceImpl(null, objectMapper, imageDownloadService);

        ModerationCheckResult result = unconfiguredService.checkImageUrl("https://cdn.capnong.vn/any.jpg");

        assertThat(result.isAgricultural()).isTrue();
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getRejectionReason()).contains("tạm thời gián đoạn");
    }

    @Test
    @DisplayName("Lỗi bảo mật SSRF -> Ném AppException, tuyệt đối không Fail-Open")
    void checkImageUrl_ssrfException_rethrowsAppException() {
        when(imageDownloadService.download(anyString()))
                .thenThrow(new AppException(ErrorCode.FILE_INVALID));

        assertThatThrownBy(() -> moderationService.checkImageUrl("http://127.0.0.1:8080/internal.png"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_INVALID);
    }

    @Test
    @DisplayName("Mức 2: Nông sản xấu mã (độ tin cậy 55%) -> Cho qua nhưng gắn cờ isFlagged=true")
    void checkImageUrl_uglyProduce_allowsUploadWithFlag() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        EntityAnnotation uglyFruit = EntityAnnotation.newBuilder()
                .setDescription("Fruit")
                .setScore(0.55f)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(uglyFruit), null))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/ugly-melon.jpg", "Dưa hấu Long An");

        // Mức 2: Vẫn là nông sản
        assertThat(result.isAgricultural()).isTrue();
        // Gắn cờ cảnh báo
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getRejectionReason()).contains("xấu mã");
    }

    @Test
    @DisplayName("Mức 2: Nghi ngờ lệch loại sản phẩm (Tên Dưa hấu nhưng ảnh Cà chua 95%) -> Cho qua nhưng gắn cờ isFlagged=true")
    void checkImageUrl_mismatchProduce_allowsUploadWithFlag() {
        when(imageDownloadService.download(anyString())).thenReturn(new byte[]{1, 2, 3});

        EntityAnnotation tomato = EntityAnnotation.newBuilder()
                .setDescription("Tomato")
                .setScore(0.95f)
                .build();

        BatchAnnotateImagesResponse batchRes = BatchAnnotateImagesResponse.newBuilder()
                .addResponses(buildAnnotateResponse(List.of(tomato), null))
                .build();

        when(imageAnnotatorClient.batchAnnotateImages(anyList())).thenReturn(batchRes);

        ModerationCheckResult result = moderationService.checkImageUrl("https://cdn.capnong.vn/tomato.jpg", "Dưa hấu Long An");

        // Mức 2: Vẫn cho phép upload (không làm nông dân bị kẹt)
        assertThat(result.isAgricultural()).isTrue();
        // Nhưng gắn cờ nghi ngờ lệch loại
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.getRejectionReason()).contains("Nghi ngờ ảnh chưa khớp với tên sản phẩm");
        assertThat(result.getRejectionReason()).contains("Dưa hấu");
        assertThat(result.getRejectionReason()).contains("Cà chua");
    }
}
