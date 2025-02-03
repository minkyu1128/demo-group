package com.example.fcm.service.sample;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

@Service
public class TopicMessageSender implements FcmSender {

    @Override
    public void send() throws FirebaseMessagingException {
        // The topic name can be optionally prefixed with "/topics/".
        String topic = "highScores";

        // See documentation on defining a message payload.
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle("Topic Push Message")
                        .setBody("FCM을 이용한 Topic Push 메시지 입니다!")
                        .build())
                .putData("score", "850")
                .putData("time", "2:45")
                .setTopic(topic)
                .build();

        // Send a message to the devices subscribed to the provided topic.
        String response = FirebaseMessaging.getInstance().send(message);

        // Response is a message ID string.
        System.out.println("Successfully sent message: " + response);
    }
}
