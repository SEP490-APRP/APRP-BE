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

        // Limit file size (e.g. 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            log.error("Upload failed: File size {} exceeds 10MB limit", file.getSize());
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
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            log.info("Deleting image from Cloudinary: {}", publicId);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.warn("Failed to delete image from Cloudinary with publicId: {}", publicId, e);
        }
    }
}
