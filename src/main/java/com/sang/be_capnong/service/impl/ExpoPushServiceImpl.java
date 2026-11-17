package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.entity.UserDeviceToken;
import com.sang.be_capnong.repository.UserDeviceTokenRepository;
import com.sang.be_capnong.service.ExpoPushService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExpoPushServiceImpl implements ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final int BATCH_SIZE = 100;

    RestTemplate restTemplate;
    ObjectMapper objectMapper;
    UserDeviceTokenRepository userDeviceTokenRepository;

    @Override
    @Async("pushNotificationExecutor")
    public void sendPushToUserAsync(Integer userId, String title, String body, Map<String, Object> data, String channelId) {
        if (userId == null) return;
        List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUser_IdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("[PushNotification] No active device tokens found for userId={}", userId);
            return;
        }
        List<String> tokenStrings = tokens.stream()
                .map(UserDeviceToken::getDeviceToken)
                .filter(this::isValidExpoPushToken)
                .distinct()
                .collect(Collectors.toList());

        if (!tokenStrings.isEmpty()) {
            sendPushToTokens(tokenStrings, title, body, data, channelId);
        }
    }

    @Override
    @Async("pushNotificationExecutor")
    public void sendPushToUsersAsync(Collection<Integer> userIds, String title, String body, Map<String, Object> data, String channelId) {
        if (userIds == null || userIds.isEmpty()) return;
        List<UserDeviceToken> tokens = userDeviceTokenRepository.findAllByUserIdsAndIsActiveTrue(userIds);
        if (tokens.isEmpty()) return;

        List<String> tokenStrings = tokens.stream()
                .map(UserDeviceToken::getDeviceToken)
                .filter(this::isValidExpoPushToken)
                .distinct()
                .collect(Collectors.toList());

        if (!tokenStrings.isEmpty()) {
            sendPushToTokens(tokenStrings, title, body, data, channelId);
        }
    }

    @Override
    @Async("pushNotificationExecutor")
    public void sendPushToRolesAsync(Collection<String> roleNames, String title, String body, Map<String, Object> data, String channelId) {
        if (roleNames == null || roleNames.isEmpty()) return;
        List<UserDeviceToken> tokens = userDeviceTokenRepository.findAllByRoleNamesAndIsActiveTrue(roleNames);
        if (tokens.isEmpty()) return;

        List<String> tokenStrings = tokens.stream()
                .map(UserDeviceToken::getDeviceToken)
                .filter(this::isValidExpoPushToken)
                .distinct()
                .collect(Collectors.toList());

        if (!tokenStrings.isEmpty()) {
            sendPushToTokens(tokenStrings, title, body, data, channelId);
        }
    }

    @Override
    @Async("pushNotificationExecutor")
    public void sendPushToTokensAsync(Collection<String> tokens, String title, String body, Map<String, Object> data, String channelId) {
        if (tokens == null || tokens.isEmpty()) return;
        List<String> validTokens = tokens.stream()
                .filter(this::isValidExpoPushToken)
                .distinct()
                .collect(Collectors.toList());

        if (!validTokens.isEmpty()) {
            sendPushToTokens(validTokens, title, body, data, channelId);
        }
    }

    private void sendPushToTokens(List<String> tokens, String title, String body, Map<String, Object> data, String channelId) {
        // Expo Push API accepts batches of up to 100 messages
        for (int i = 0; i < tokens.size(); i += BATCH_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + BATCH_SIZE, tokens.size()));
            sendBatch(batch, title, body, data, channelId);
        }
    }

    private void sendBatch(List<String> tokens, String title, String body, Map<String, Object> data, String channelId) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>(tokens.size());
            for (String token : tokens) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("to", token);
                msg.put("title", title);
                msg.put("body", body);
                msg.put("sound", "default");
                msg.put("priority", "high");
                if (channelId != null && !channelId.isBlank()) {
                    msg.put("channelId", channelId);
                }
                if (data != null && !data.isEmpty()) {
                    msg.put("data", data);
                }
                messages.add(msg);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);

            log.info("[ExpoPush] Sending batch of {} push notifications. Title: '{}'", tokens.size(), title);
            ResponseEntity<String> response = restTemplate.postForEntity(EXPO_PUSH_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                handleExpoResponse(response.getBody(), tokens);
            } else {
                log.warn("[ExpoPush] Received non-200 response: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("[ExpoPush] Failed to dispatch push notifications: {}", e.getMessage(), e);
        }
    }

    @Transactional
    protected void handleExpoResponse(String responseBody, List<String> sentTokens) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isArray()) {
                List<String> deadTokens = new ArrayList<>();
                for (int i = 0; i < dataNode.size(); i++) {
                    JsonNode receipt = dataNode.get(i);
                    String status = receipt.path("status").asText();
                    if ("error".equalsIgnoreCase(status)) {
                        String errorType = receipt.path("details").path("error").asText();
                        log.warn("[ExpoPush] Ticket error for token: {}. Error: {}", 
                                i < sentTokens.size() ? sentTokens.get(i) : "unknown", errorType);
                        if ("DeviceNotRegistered".equalsIgnoreCase(errorType)) {
                            if (i < sentTokens.size()) {
                                deadTokens.add(sentTokens.get(i));
                            }
                        }
                    }
                }

                if (!deadTokens.isEmpty()) {
                    log.info("[ExpoPush] Deactivating {} unregistered device tokens", deadTokens.size());
                    userDeviceTokenRepository.deactivateTokens(deadTokens, LocalDateTime.now());
                }
            }
        } catch (Exception e) {
            log.debug("[ExpoPush] Error parsing Expo response tickets: {}", e.getMessage());
        }
    }

    private boolean isValidExpoPushToken(String token) {
        if (token == null || token.isBlank()) return false;
        return token.startsWith("ExponentPushToken[") || token.startsWith("ExpoPushToken[");
    }
}

