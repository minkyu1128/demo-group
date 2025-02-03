package com.example.fcm.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TopicSubscribeRequest {
    private String topic;
    private List<String> token;
} 