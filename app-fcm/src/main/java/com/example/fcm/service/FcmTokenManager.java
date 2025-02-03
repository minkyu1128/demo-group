package com.example.fcm.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 클라이언트 FCM 토큰 관리자
 */
@Component
public class FcmTokenManager {
    private final ConcurrentHashMap<String, String> clientTokenMap = new ConcurrentHashMap<>();


    public void registerToken(String token, String clientId) {
        clientTokenMap.put(token, clientId);
    }

    public void removeToken(String token) {
        clientTokenMap.remove(token);
    }

    public String getClientId(String token) {
        return clientTokenMap.get(token);
    }

    public List<String> getTokens() {
        return clientTokenMap.entrySet().stream()
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());
    }

    public String getToken(String clientId) {
        for (String token : clientTokenMap.keySet()) {
            if (clientTokenMap.get(token).equals(clientId)) {
                return token;
            }
        }
        return null;
    }

    public boolean isTokenRegistered(String token) {
        return clientTokenMap.containsKey(token);
    }

    public boolean isClientIdRegistered(String clientId) {
        return clientTokenMap.containsValue(clientId);
    }

    public boolean isTokenRegisteredToClientId(String token, String clientId) {
        return clientTokenMap.get(token).equals(clientId);
    }

}
