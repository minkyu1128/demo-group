package com.example.fcm.service;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.List;

public interface TopicSubscribeManageService {
    /**
     * 토픽을 구독하도록 등록 합니다.
     *
     * @param topic
     * @param registrationTokens
     */
    void subscribeTopic(String topic, List<String> registrationTokens) throws FirebaseMessagingException;

    /**
     * 토픽 구독을 취소 합니다.
     *
     * @param topic
     * @param registrationTokens
     */
    void unsubscribeTopic(String topic, List<String> registrationTokens) throws FirebaseMessagingException;
}
