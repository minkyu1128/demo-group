package com.example.fcm.service.impl;

import com.example.fcm.service.FcmSendService;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 여러 기기에 메시지 전송
 */
@Service
public class MulticastMessageSender implements FcmSendService {
    @Override
    public void send() throws FirebaseMessagingException {
        List<String> registrationTokens = Arrays.asList(
                "YOUR_REGISTRATION_TOKEN_1",
                // ...
                "YOUR_REGISTRATION_TOKEN_n"
        );

        // 각 토큰별로 메시지 객체 생성
        List<Message> messages = registrationTokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle("Multicast Push Message")
                                .setBody("FCM을 이용한 Multicast Push 메시지 입니다!")
                                .build())
                        .putData("score", "850")
                        .putData("time", "2:45")
                        .build())
                .collect(Collectors.toList());

        try {
            // sendMulticast 대신 sendEach 사용
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();

                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        failedTokens.add(registrationTokens.get(i));
                        // 실패 원인 로깅 추가
                        System.out.println("Error for token " + registrationTokens.get(i) +
                                ": " + responses.get(i).getException().getMessage());
                    }
                }

                System.out.println("List of tokens that caused failures: " + failedTokens);
            }

            System.out.println("Successfully sent messages: " + response.getSuccessCount());
        } catch (FirebaseMessagingException e) {
            System.err.println("Failed to send messages: " + e.getMessage());
        }

    }

    /** 2024.06월부로 Mulicast 전송 서비스 중단. HTTP v1 API 으로 전환 필요 */
//    @Override
//    public void send() throws FirebaseMessagingException {
//        // These registration tokens come from the client FCM SDKs.
//        //호출당 최대 500개의 기기 등록 토큰을 지정 할 수 있습니다.
//        List<String> registrationTokens = Arrays.asList(
//                "YOUR_REGISTRATION_TOKEN_1",
////                        // ...
//                        "YOUR_REGISTRATION_TOKEN_n"
//        );
//
//        MulticastMessage message = MulticastMessage.builder()
//                .setNotification(Notification.builder()
//                        .setTitle("Multicast Push Message")
//                        .setBody("FCM을 이용한 Multicast Push 메시지 입니다!")
//                        .build())
//                .putData("score", "850")
//                .putData("time", "2:45")
//                .addAllTokens(registrationTokens)
//                .build();
//
//
//        //반환 값은 입력 토큰 순서와 일치하는 토큰 목록입니다. 오류가 발생한 토큰을 확인하려는 경우에 유용합니다.
//        BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
//        if (response.getFailureCount() > 0) {
//            List<SendResponse> responses = response.getResponses();
//            List<String> failedTokens = new ArrayList<>();
//            for (int i = 0; i < responses.size(); i++) {
//                if (!responses.get(i).isSuccessful()) {
//                    // The order of responses corresponds to the order of the registration tokens.
//                    failedTokens.add(registrationTokens.get(i));
//                }
//            }
//
//            System.out.println("List of tokens that caused failures: " + failedTokens);
//        }
//    }
}
