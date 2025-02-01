package com.example.fcm.service;

import com.google.firebase.messaging.FirebaseMessagingException;

public interface FcmSendService {
    void send() throws FirebaseMessagingException;
}
