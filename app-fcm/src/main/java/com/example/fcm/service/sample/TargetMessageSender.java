package com.example.fcm.service.sample;

import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

@Service
public class TargetMessageSender implements FcmSender {
    @Override
    public void send() throws FirebaseMessagingException {
        // This registration token comes from the client FCM SDKs.
//        String registrationToken = "YOUR_REGISTRATION_TOKEN";
        String registrationToken = "cLSSEnjrZnWQw4hZ2IJ6w4:APA91bETb3rygK-RQ-BEClOskdom3ckfo6wlNb2T7KF4S1TH29-Q0sjtLo5gIHb4IZUFC_NSslFknCzTzyVMc0u8WazLHswFjCiCyS_Lu8TkHasbeUkqrVo";

        // See documentation on defining a message payload.
        Message message = Message.builder()
//                .setNotification(Notification.builder()
//                        .setTitle("Target Push Message")
//                        .setBody("FCM을 이용한 Target Push 메시지 입니다!")
//                        .build())
                .putData("score", "850")
                .putData("time", "2:45")
                .setToken(registrationToken)
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setTitle("웹푸시 Target Push Message")
                                .setBody("웹푸시 FCM을 이용한 Target Push 메시지 입니다!")
                                .setDirection(WebpushNotification.Direction.LEFT_TO_RIGHT)
                                .setIcon("https://firebase.google.com/images/social.png")
                                .build())
                        .setFcmOptions(WebpushFcmOptions.builder()
                                .setLink("https://www.naver.com")
                                .build())
                        .build())
                .build();

        try {
            // Send a message to the device corresponding to the provided
            // registration token.
            String response = FirebaseMessaging.getInstance().send(message);
            // Response is a message ID string.
            System.out.println("Successfully sent message: " + response);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED //토큰이 더 이상 유효하지 않음
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT //토큰이 잘못되었거나, 앱이 삭제됨
            ) {
                System.out.println("Removed invalid token: " + registrationToken);
            }
            System.err.println("Failed to send message: " + e.getMessage());
            throw e;
        }
    }
}
