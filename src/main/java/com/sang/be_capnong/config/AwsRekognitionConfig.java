package com.sang.be_capnong.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;

@Configuration
@Slf4j
public class AwsRekognitionConfig {

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:ap-southeast-1}")
    private String region;

    @Bean
    public RekognitionClient rekognitionClient() {
        Region awsRegion = Region.of(region);
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            log.info("Initializing AWS RekognitionClient with static credentials in region: {}", region);
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            return RekognitionClient.builder()
                    .region(awsRegion)
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }

        log.warn("AWS access key or secret key not configured. Fallback to DefaultCredentialsProvider.");
        return RekognitionClient.builder()
                .region(awsRegion)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
