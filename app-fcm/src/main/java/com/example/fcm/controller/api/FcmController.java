package com.example.fcm.controller.api;

import com.example.fcm.dto.FcmMessageRequest;
import com.example.fcm.service.FcmService;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = {"/api/v1/fcm"})
public class FcmController {
    private final FcmService fcmService;

    public FcmController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    /**
     * FCM 메시지를 특정 사용자에게 전송합니다.
     *
     * @return ResponseEntity<String>
     * @throws FirebaseMessagingException
     */
    @PostMapping("/targets/{registrationToken}")
    public ResponseEntity<String> targetSend(@RequestBody FcmMessageRequest request, @PathVariable String registrationToken) throws FirebaseMessagingException {
        fcmService.sendTargetMessage(request, registrationToken);
        return ResponseEntity.ok().build();
    }

    /**
     * FCM 메시지를 여러 사용자에게 전송합니다.
     *
     * @return
     * @throws FirebaseMessagingException
     */
    @PostMapping("/multicast")
    public ResponseEntity<String> multicastSend(@RequestBody FcmMessageRequest request) throws FirebaseMessagingException {
        fcmService.sendMulticastMessage(request);
        return ResponseEntity.ok().build();
    }

    /**
     * FCM 메시지를 토픽에 등록된 사용자에게 전송합니다.
     *
     * @return
     * @throws FirebaseMessagingException
     */
    @PostMapping("/topics/{topic}")
    public ResponseEntity<String> topicSend(@RequestBody FcmMessageRequest request, @PathVariable String topic) throws FirebaseMessagingException {
        fcmService.sendTopicMessage(request, topic);
        return ResponseEntity.ok().build();
    }
}
