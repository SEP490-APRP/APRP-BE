package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.ImageDownloadService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ImageDownloadServiceImpl implements ImageDownloadService {

    @Value("${google.vision.max-file-size-bytes:5242880}")
    long maxFileSizeBytes = 5242880L;

    @Value("${google.vision.connect-timeout-seconds:5}")
    int connectTimeoutSeconds = 5;

    @Value("${google.vision.read-timeout-seconds:10}")
    int readTimeoutSeconds = 10;

    @Override
    public byte[] download(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new AppException(ErrorCode.IMAGE_URL_REQUIRED);
        }

        URI uri = validateAndParseUri(imageUrl.trim());
        validateHostAndIp(uri.getHost());

        int effectiveConnectTimeout = connectTimeoutSeconds > 0 ? connectTimeoutSeconds : 5;
        int effectiveReadTimeout = readTimeoutSeconds > 0 ? readTimeoutSeconds : 10;

        // Cấu hình HttpClient với Redirect.NEVER để chống triệt để tấn công SSRF qua 301/302 Redirect
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(effectiveConnectTimeout))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(effectiveReadTimeout))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() >= 400) {
                log.warn("Image download returned HTTP error status {}: {}", response.statusCode(), imageUrl);
                throw new AppException(ErrorCode.FILE_INVALID);
            }

            // Chặn chuyển hướng 3xx thủ công nếu có
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                log.warn("Blocked HTTP redirect attempt (status {}) for URL: {}", response.statusCode(), imageUrl);
                throw new AppException(ErrorCode.FILE_INVALID);
            }

            byte[] body = response.body();
            if (body == null || body.length == 0) {
                log.warn("Downloaded image is empty from URL: {}", imageUrl);
                throw new AppException(ErrorCode.FILE_INVALID);
            }

            if (body.length > maxFileSizeBytes) {
                log.warn("Image file size exceeds limit ({} bytes > {} bytes): {}", body.length, maxFileSizeBytes, imageUrl);
                throw new AppException(ErrorCode.FILE_TOO_LARGE);
            }

            return body;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted while downloading image: {}", imageUrl, e);
            throw new AppException(ErrorCode.IMAGE_MODERATION_FAILED);
        } catch (IOException e) {
            log.warn("I/O error occurred while downloading image from URL {}: {}", imageUrl, e.getMessage());
            throw new AppException(ErrorCode.FILE_INVALID);
        }
    }

    private URI validateAndParseUri(String trimmedUrl) {
        URI uri;
        try {
            uri = URI.create(trimmedUrl);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid URI syntax: {}", trimmedUrl);
            throw new AppException(ErrorCode.FILE_INVALID);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            log.warn("Unsupported URI scheme (only http/https allowed): {}", scheme);
            throw new AppException(ErrorCode.FILE_INVALID);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            log.warn("Missing host in URL: {}", trimmedUrl);
            throw new AppException(ErrorCode.FILE_INVALID);
        }

        return uri;
    }

    private void validateHostAndIp(String host) {
        String trimmedHost = host.trim().toLowerCase();

        // 1. Kiểm tra nhanh chuỗi host phổ biến
        if (trimmedHost.equals("localhost") || trimmedHost.endsWith(".localhost")
                || trimmedHost.equals("127.0.0.1") || trimmedHost.equals("0.0.0.0")
                || trimmedHost.equals("169.254.169.254") || trimmedHost.equals("::1")) {
            log.warn("Blocked explicit SSRF host string: {}", host);
            throw new AppException(ErrorCode.FILE_INVALID);
        }

        // 2. Phân giải DNS thực tế để chống DNS Rebinding / IP bypass
        try {
            InetAddress[] addresses = InetAddress.getAllByName(trimmedHost);
            for (InetAddress address : addresses) {
                if (isPrivateOrLocalAddress(address)) {
                    log.warn("Blocked SSRF attempt: host '{}' resolves to private/local IP: {}",
                            host, address.getHostAddress());
                    throw new AppException(ErrorCode.FILE_INVALID);
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Unable to resolve host for image URL: {}", host);
            throw new AppException(ErrorCode.FILE_INVALID);
        }
    }

    private boolean isPrivateOrLocalAddress(InetAddress address) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress()
                || address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
            return true;
        }

        // Kiểm tra bổ sung cho địa chỉ Cloud Instance Metadata IPv4 (169.254.169.254 - IMDS) hoặc Carrier-grade NAT (100.64.0.0/10)
        String ip = address.getHostAddress();
        if (ip.startsWith("169.254.") || ip.startsWith("127.") || ip.startsWith("10.")
                || ip.startsWith("192.168.") || ip.startsWith("0.")) {
            return true;
        }

        return false;
    }
}
