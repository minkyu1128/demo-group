package com.example.fcm.controller.api;

import com.example.fcm.dto.TokenRegistrationRequest;
import com.example.fcm.service.FcmTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fcm/token")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenManager clientTokenManager;

    /**
     * FCM 클라이언트 토큰을 등록합니다.
     *
     * @param request
     * @return ResponseEntity<Void>
     */
    @PostMapping("")
    public ResponseEntity<Void> registerToken(@RequestBody TokenRegistrationRequest request) {
        clientTokenManager.registerToken(request.getToken(), request.getClientId());
        return ResponseEntity.ok().build();
    }

    /**
     * FCM 클라이언트 토큰을 해제합니다.
     *
     * @param request
     * @return ResponseEntity<Void>
     */
    @DeleteMapping("")
    public ResponseEntity<Void> unregisterToken(@RequestBody TokenRegistrationRequest request) {
        if (clientTokenManager.isTokenRegisteredToClientId(request.getToken(), request.getClientId())) {
            clientTokenManager.removeToken(request.getToken());
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
} 