package com.sang.be_capnong.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.CloudinaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryServiceImpl implements CloudinaryService {

    Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            log.error("Upload failed: File is null or empty");
            throw new AppException(ErrorCode.FILE_INVALID);
        }

        boolean isVideo = false;
        if (file.getContentType() != null && file.getContentType().startsWith("video/")) {
            isVideo = true;
        } else if (file.getOriginalFilename() != null) {
            String name = file.getOriginalFilename().toLowerCase();
            if (name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".avi") || name.endsWith(".mkv")) {
                isVideo = true;
            }
        }

        long maxSize = isVideo ? 50L * 1024 * 1024 : 10L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            log.error("Upload failed: File size {} exceeds {}MB limit", file.getSize(), isVideo ? 50 : 10);
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        try {
            log.info("Uploading file '{}' (size: {} bytes) to Cloudinary folder '{}'",
                    file.getOriginalFilename(), file.getSize(), folder);

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder != null && !folder.isBlank() ? folder : "capnong",
                            "resource_type", "auto"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Uploaded successfully to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Cloudinary upload failed with IOException: ", e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("Cloudinary upload encountered unexpected error: ", e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        deleteMedia(publicId, false);
    }

    public void deleteMedia(String publicId, boolean isVideo) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            log.info("Deleting {} from Cloudinary: {}", isVideo ? "video" : "image", publicId);
            java.util.Map<String, Object> params = isVideo
                    ? java.util.Map.of("resource_type", "video")
                    : ObjectUtils.emptyMap();
            cloudinary.uploader().destroy(publicId, params);
        } catch (Exception e) {
            log.warn("Failed to delete media from Cloudinary with publicId: {}", publicId, e);
        }
    }

    @Override
    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String publicId = extractPublicId(imageUrl);
        if (publicId != null && !publicId.isBlank()) {
            boolean isVideo = imageUrl.contains("/video/upload/") ||
                    imageUrl.toLowerCase().matches(".*\\.(mp4|mov|webm|avi|mkv)(\\?.*)?$");
            deleteMedia(publicId, isVideo);
        } else {
            log.debug("Skipping Cloudinary deletion: URL is not a recognized Cloudinary resource: {}", imageUrl);
        }
    }

    @Override
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            String pathAfterUpload = imageUrl.substring(uploadIndex + "/upload/".length());
            int queryIndex = pathAfterUpload.indexOf('?');
            if (queryIndex != -1) {
                pathAfterUpload = pathAfterUpload.substring(0, queryIndex);
            }

            String[] segments = pathAfterUpload.split("/");
            int startIndex = 0;
            for (int i = 0; i < segments.length; i++) {
                String seg = segments[i];
                if (seg.matches("^v\\d+$")) {
                    startIndex = i + 1;
                    break;
                } else if (seg.contains(",") || seg.startsWith("c_") || seg.startsWith("w_") || seg.startsWith("h_") || seg.startsWith("q_")) {
                    startIndex = i + 1;
                }
            }

            if (startIndex >= segments.length) {
                return null;
            }

            StringBuilder publicIdBuilder = new StringBuilder();
            for (int i = startIndex; i < segments.length; i++) {
                if (publicIdBuilder.length() > 0) {
                    publicIdBuilder.append("/");
                }
                publicIdBuilder.append(segments[i]);
            }

            String fullPublicId = publicIdBuilder.toString();
            int lastDot = fullPublicId.lastIndexOf('.');
            if (lastDot != -1) {
                fullPublicId = fullPublicId.substring(0, lastDot);
            }

            return fullPublicId.isBlank() ? null : fullPublicId;
        } catch (Exception e) {
            log.warn("Failed to extract Cloudinary publicId from URL: {}", imageUrl, e);
            return null;
        }
    }
}
