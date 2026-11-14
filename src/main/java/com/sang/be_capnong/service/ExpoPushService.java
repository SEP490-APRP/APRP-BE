package com.sang.be_capnong.service;

import java.util.Collection;
import java.util.Map;

public interface ExpoPushService {

    void sendPushToTokensAsync(Collection<String> tokens, String title, String body, Map<String, Object> data, String channelId);

    void sendPushToUserAsync(Integer userId, String title, String body, Map<String, Object> data, String channelId);

    void sendPushToUsersAsync(Collection<Integer> userIds, String title, String body, Map<String, Object> data, String channelId);

    void sendPushToRolesAsync(Collection<String> roleNames, String title, String body, Map<String, Object> data, String channelId);
}

