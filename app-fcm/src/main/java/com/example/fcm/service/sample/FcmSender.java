package com.example.fcm.service.sample;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface FcmSender {
    void send() throws FirebaseMessagingException;
}
