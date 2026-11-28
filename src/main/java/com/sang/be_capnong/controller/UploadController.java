package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.ModerationCheckResult;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.UploadResponse;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.CloudinaryService;
import com.sang.be_capnong.service.ImageModerationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UploadController {

    CloudinaryService cloudinaryService;
    ImageModerationService imageModerationService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            @RequestParam(value = "moderate", defaultValue = "false") boolean moderate,
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "categoryId", required = false) Integer categoryId
    ) {
        log.info("REST request to upload image to folder: {} (moderate={})", folder, moderate);
        String url = cloudinaryService.uploadImage(file, folder);

        if (moderate) {
            ModerationCheckResult checkResult = imageModerationService.checkImageUrl(url, productName, categoryId);
            if (!checkResult.isAgricultural()) {
                log.warn("Uploaded image rejected by AI moderation (folder={}, reason={})", folder, checkResult.getRejectionReason());
                cloudinaryService.deleteImageByUrl(url);
                throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
            }
        }

        return ApiResponse.<UploadResponse>builder()
                .result(UploadResponse.builder().url(url).build())
                .build();
    }

    @PostMapping(value = {"/video", "/media"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "chat") String folder
    ) {
        log.info("REST request to upload video/media to folder: {}", folder);
        String url = cloudinaryService.uploadImage(file, folder);

        return ApiResponse.<UploadResponse>builder()
                .result(UploadResponse.builder().url(url).build())
                .build();
    }
}
