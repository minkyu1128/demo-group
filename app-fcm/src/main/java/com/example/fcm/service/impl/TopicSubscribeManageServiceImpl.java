package com.example.fcm.service.impl;

import com.example.fcm.service.TopicSubscribeManageService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.TopicManagementResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicSubscribeManageServiceImpl implements TopicSubscribeManageService {
    @Override
    public void subscribeTopic(String topic, List<String> tokens) throws FirebaseMessagingException {
        // These registration tokens come from the client FCM SDKs.
//        List<String> registrationTokens = Arrays.asList(
//                "YOUR_REGISTRATION_TOKEN_1",
//                // ...
//                "YOUR_REGISTRATION_TOKEN_n"
//        );
        List<String> registrationTokens = tokens;

        // Subscribe the devices corresponding to the registration tokens to the
        // topic.
        TopicManagementResponse response = FirebaseMessaging.getInstance().subscribeToTopic(registrationTokens, topic);
        // See the TopicManagementResponse reference documentation
        // for the contents of response.
        System.out.println(response.getSuccessCount() + " tokens were subscribed successfully");
    }

    @Override
    public void unsubscribeTopic(String topic, List<String> tokens) throws FirebaseMessagingException {
        // These registration tokens come from the client FCM SDKs.
//        List<String> registrationTokens = Arrays.asList(
//                "YOUR_REGISTRATION_TOKEN_1",
//                // ...
//                "YOUR_REGISTRATION_TOKEN_n"
//        );
        List<String> registrationTokens = tokens;

        // Unsubscribe the devices corresponding to the registration tokens from
        // the topic.
        TopicManagementResponse response = FirebaseMessaging.getInstance().unsubscribeFromTopic(registrationTokens, topic);
        // See the TopicManagementResponse reference documentation
        // for the contents of response.
        System.out.println(response.getSuccessCount() + " tokens were unsubscribed successfully");
    }
}
