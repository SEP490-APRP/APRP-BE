package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.UploadResponse;
import com.sang.be_capnong.service.CloudinaryService;
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

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder
    ) {
        log.info("REST request to upload image to folder: {}", folder);
        String url = cloudinaryService.uploadImage(file, folder);

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
