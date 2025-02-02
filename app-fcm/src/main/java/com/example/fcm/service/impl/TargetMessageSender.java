package com.example.fcm.service.impl;

import com.example.fcm.service.FcmSendService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class TargetMessageSender implements FcmSendService {

    @Override
    public void send() throws FirebaseMessagingException {
        // This registration token comes from the client FCM SDKs.
        String registrationToken = "YOUR_REGISTRATION_TOKEN";

        // See documentation on defining a message payload.
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle("Target Push Message")
                        .setBody("FCM을 이용한 Target Push 메시지 입니다!")
                        .build())
                .putData("score", "850")
                .putData("time", "2:45")
                .setToken(registrationToken)
                .build();

        // Send a message to the device corresponding to the provided
        // registration token.
        String response = FirebaseMessaging.getInstance().send(message);
        // Response is a message ID string.
        System.out.println("Successfully sent message: " + response);
    }
}
