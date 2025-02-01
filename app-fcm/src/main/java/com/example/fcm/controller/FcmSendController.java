package com.example.fcm.controller;

import com.example.fcm.service.FcmSendService;
import com.example.fcm.service.impl.MulticastMessageSender;
import com.example.fcm.service.impl.TargetMessageSender;
import com.example.fcm.service.impl.TopicMessageSender;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = {"/api/v1/fcm"})
public class FcmSendController {
    private final FcmSendService targetMesssage;
    private final FcmSendService multicastMessage;
    private final FcmSendService topicMessage;

    public FcmSendController(TargetMessageSender targetMesssage, MulticastMessageSender multicastMessage, TopicMessageSender topicMessage) {
        this.targetMesssage = targetMesssage;
        this.multicastMessage = multicastMessage;
        this.topicMessage = topicMessage;
    }


    @PostMapping("/target")
    public ResponseEntity<String> targetSend() throws FirebaseMessagingException {
        targetMesssage.send();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/multicast")
    public ResponseEntity<String> multicastSend() throws FirebaseMessagingException {
        multicastMessage.send();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/topic")
    public ResponseEntity<String> topicSend() throws FirebaseMessagingException {
        topicMessage.send();
        return ResponseEntity.ok().build();
    }
}
