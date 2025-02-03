package com.example.fcm.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRegistrationRequest {
    private String token;
    private String clientId;
} 