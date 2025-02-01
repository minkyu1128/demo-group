package com.example.fcm.service.impl;

import com.example.fcm.service.FcmSendService;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 여러 기기에 메시지 전송
 */
@Service
public class MulticastMessageSender implements FcmSendService {

    @Override
    public void send() throws FirebaseMessagingException {
        // These registration tokens come from the client FCM SDKs.
        //호출당 최대 500개의 기기 등록 토큰을 지정 할 수 있습니다.
        List<String> registrationTokens = Arrays.asList(
                "YOUR_REGISTRATION_TOKEN_1",
                // ...
                "YOUR_REGISTRATION_TOKEN_n"
        );

        MulticastMessage message = MulticastMessage.builder()
                .putData("score", "850")
                .putData("time", "2:45")
                .addAllTokens(registrationTokens)
                .build();


        //반환 값은 입력 토큰 순서와 일치하는 토큰 목록입니다. 오류가 발생한 토큰을 확인하려는 경우에 유용합니다.
        BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            List<String> failedTokens = new ArrayList<>();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    // The order of responses corresponds to the order of the registration tokens.
                    failedTokens.add(registrationTokens.get(i));
                }
            }

            System.out.println("List of tokens that caused failures: " + failedTokens);
        }
    }
}
