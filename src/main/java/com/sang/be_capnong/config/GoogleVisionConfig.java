
package com.sang.be_capnong.config;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;

@Configuration
@Slf4j
public class GoogleVisionConfig {

    @Value("${google.vision.credentials-path:}")
    private String credentialsPath;

    private final ResourceLoader resourceLoader;

    public GoogleVisionConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Khởi tạo bean ImageAnnotatorClient cho Google Cloud Vision SDK.
     * Hỗ trợ nạp file Service Account JSON từ classpath (e.g. classpath:credentials.json),
     * đường dẫn file vật lý (e.g. file:D:/keys/credentials.json),
     * hoặc tự động nhận diện từ biến môi trường GOOGLE_APPLICATION_CREDENTIALS.
     */
    @Bean(destroyMethod = "close")
    public ImageAnnotatorClient imageAnnotatorClient() {
        try {
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                log.info("Loading Google Cloud credentials from path: {}", credentialsPath);
                Resource resource = resourceLoader.getResource(credentialsPath);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        GoogleCredentials credentials = GoogleCredentials.fromStream(is);
                        ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                                .build();
                        log.info("Successfully initialized ImageAnnotatorClient with credentials: {}", credentialsPath);
                        return ImageAnnotatorClient.create(settings);
                    }
                } else {
                    log.warn("Credentials file not found at: {}. Falling back to Application Default Credentials.", credentialsPath);
                }
            }

            log.info("Initializing ImageAnnotatorClient with Application Default Credentials (GOOGLE_APPLICATION_CREDENTIALS)...");
            return ImageAnnotatorClient.create();
        } catch (Exception e) {
            log.warn("Could not initialize ImageAnnotatorClient: {}. AI Moderation will fallback to Controlled Fail-Open if credentials are not configured.", e.getMessage());
            return null;
        }
    }
}
