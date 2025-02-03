package com.example.fcm.controller.api;

import com.example.fcm.dto.TopicSubscribeRequest;
import com.example.fcm.service.TopicSubscribeManageService;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = {"/api/v1/fcm/topics"})
public class TopicSubscribeManageController {
    private final TopicSubscribeManageService subscribeManageService;

    public TopicSubscribeManageController(TopicSubscribeManageService subscribeManageService) {
        this.subscribeManageService = subscribeManageService;
    }


    /**
     * 토픽 구독
     *
     * @param request
     * @return ResponseEntity<Void>
     * @throws FirebaseMessagingException
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribeTopic(@RequestBody TopicSubscribeRequest request) throws FirebaseMessagingException {
        subscribeManageService.subscribeTopic(request.getTopic(), request.getToken());
        return ResponseEntity.ok().build();
    }

    /**
     * 토픽 구독 해지
     *
     * @param request
     * @return ResponseEntity<Void>
     * @throws FirebaseMessagingException
     */
    @DeleteMapping("/unsubscriptions")
    public ResponseEntity<Void> unsubscribeTopic(@RequestBody TopicSubscribeRequest request) throws FirebaseMessagingException {
        subscribeManageService.unsubscribeTopic(request.getTopic(), request.getToken());
        return ResponseEntity.ok().build();
    }

}
