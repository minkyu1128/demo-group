package com.example.fcm.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebPushConfigRequest {
    private String icon;
    private String link;
    // 기타 웹푸시 관련 설정들...
}
