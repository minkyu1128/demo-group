package com.example.fcm.service.impl;

import com.example.fcm.dto.FcmMessageRequest;
import com.example.fcm.service.FcmTokenManager;
import com.example.fcm.service.FcmService;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FcmServiceImpl implements FcmService {
    private final FcmTokenManager clientTokenManager;

    public FcmServiceImpl(FcmTokenManager clientTokenManager) {
        this.clientTokenManager = clientTokenManager;
    }


    public void sendTargetMessage(FcmMessageRequest request, String registrationToken) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .putAllData(request.getData())
                .setToken(registrationToken)    // 토큰 설정
//                .setWebpushConfig(WebpushConfig.builder()
//                        .setNotification(WebpushNotification.builder()
////                                .setTitle(request.getTitle()) //설정 시 Notification의 title은 무시됨
////                                .setBody(request.getBody())   //설정 시 Notification의 body는 무시됨
//                                .setDirection(WebpushNotification.Direction.LEFT_TO_RIGHT)
//                                .setIcon(request.getWebPushConfig().getIcon())
//                                .build())
//                        .setFcmOptions(WebpushFcmOptions.builder()
//                                .setLink(request.getWebPushConfig().getLink())
//                                .build())
//                        .build())
                .build();

        sendMessage(message, registrationToken);
    }


    public void sendMulticastMessage(FcmMessageRequest request) throws FirebaseMessagingException {
        List<String> registrationTokens = clientTokenManager.getTokens();

        // 각 토큰별로 메시지 객체 생성
        List<Message> messages = registrationTokens.stream()
                .map(token -> Message.builder()
                        .setToken(token)
                        .setNotification(Notification.builder()
                                .setTitle(request.getTitle())
                                .setBody(request.getBody())
                                .build())
                        .putAllData(request.getData())
                        .build())
                .collect(Collectors.toList());

        sendMessage(messages, registrationTokens);
    }

    public void sendTopicMessage(FcmMessageRequest request, String topic) throws FirebaseMessagingException {
        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(request.getTitle())
                        .setBody(request.getBody())
                        .build())
                .putAllData(request.getData())
                .putData("topic", topic)
                .setTopic(topic)    //토픽 설정
                .build();

        sendMessage(message, (FcmSendExceptionHandler) null);
    }

    interface FcmSendExceptionHandler {
        void handleException(FirebaseMessagingException e) throws FirebaseMessagingException;
    }

    private void sendMessage(Message message, FcmSendExceptionHandler hanlder) throws FirebaseMessagingException {
        try {
            // Send a message to the device corresponding to the provided
            // registration token.
            String response = FirebaseMessaging.getInstance().send(message);
            // Response is a message ID string.
            System.out.println("Successfully sent message: " + response);
        } catch (FirebaseMessagingException e) {
            if (hanlder != null) hanlder.handleException(e);
            throw e;
        }
    }

    private void sendMessage(Message message, String registrationToken) throws FirebaseMessagingException {
        sendMessage(message, e -> {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED    //토큰이 더 이상 유효하지 않음
                    || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT //토큰이 잘못되었거나, 앱이 삭제됨
            ) {
                clientTokenManager.removeToken(registrationToken);
                System.out.println("Removed invalid token: " + registrationToken);
            }
            System.err.println("Failed to send message: " + e.getMessage());
        });
    }

    private void sendMessage(List<Message> messages, List<String> registrationTokens) throws FirebaseMessagingException {
        try {
            // sendMulticast 대신 sendEach 사용
            BatchResponse response = FirebaseMessaging.getInstance().sendEach(messages);

            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> failedTokens = new ArrayList<>();

                for (int i = 0; i < responses.size(); i++) {
                    if (!responses.get(i).isSuccessful()) {
                        failedTokens.add(registrationTokens.get(i));

                        // 실패한 토큰이 유효하지 않은 경우 (토큰 만료 등) ClientTokenManager에서 제거
                        FirebaseMessagingException exception = responses.get(i).getException();
                        if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED  //토큰이 더 이상 유효하지 않음
                                || exception.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT    //토큰이 잘못되었거나, 앱이 삭제됨
                        ) {
                            clientTokenManager.removeToken(failedTokens.get(i));
                            System.out.println("Removed invalid token: " + failedTokens.get(i));
                        }

                        // 실패 원인 로깅 추가
                        System.out.println("Error for token " + registrationTokens.get(i) + ": " + responses.get(i).getException().getMessage());
                    }
                }

                System.out.println("List of tokens that caused failures: " + failedTokens);
            }

            System.out.println("Successfully sent messages: " + response.getSuccessCount());
        } catch (FirebaseMessagingException e) {
            System.err.println("Failed to send messages: " + e.getMessage());
        }
    }
}
