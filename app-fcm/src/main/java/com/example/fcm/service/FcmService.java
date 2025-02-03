package com.example.fcm.service;

import com.example.fcm.dto.FcmMessageRequest;
import com.google.firebase.messaging.FirebaseMessagingException;

public interface FcmService {


    /**
     * 특정 기기에 메시지 전송
     */
    void sendTargetMessage(FcmMessageRequest request, String registrationToken) throws FirebaseMessagingException;

    /**
     * 여러 기기에 메시지 전송
     */
    void sendMulticastMessage(FcmMessageRequest request) throws FirebaseMessagingException;

    /**
     * 특정 주제에 메시지 전송
     */
    void sendTopicMessage(FcmMessageRequest request, String topic) throws FirebaseMessagingException;

}
