package com.example.fcm.service;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface TopicSubscribeManageService {
    /**
     * 토픽을 구독하도록 등록 합니다.
     *
     * @param topic
     * @param registrationToken
     */
    void subscribeToTopic(String topic, String registrationToken) throws FirebaseMessagingException;

    /**
     * 토픽 구독을 취소 합니다.
     *
     * @param topic
     * @param registrationToken
     */
    void unsubscribeFromTopic(String topic, String registrationToken) throws FirebaseMessagingException;
}
