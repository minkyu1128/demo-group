package com.example.fcm.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class FcmMessageRequest {
    private String title;
    private String body;
    private Map<String, String> data;

    // 토픽 메시지용
    private String topic;

    // 타겟 메시지용
    private String token;
    private List<String> tokens;

    // 웹푸시 설정
    @Builder.Default
    private WebPushConfigRequest webPushConfig = WebPushConfigRequest.builder().build();
}

