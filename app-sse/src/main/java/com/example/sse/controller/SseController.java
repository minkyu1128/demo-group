package com.example.sse.controller;

import com.example.sse.model.SseEvent;
import com.example.sse.service.SseEmitterService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE(Server-Sent Events) 관련 엔드포인트를 제공하는 REST 컨트롤러
 * 클라이언트의 SSE 연결 요청 처리 및 이벤트 전송을 담당
 */
@RestController
@RequestMapping("/api/v1/sse")
public class SseController {

    private final SseEmitterService sseEmitterService;

    public SseController(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    /**
     * 클라이언트의 SSE 연결 요청을 처리하는 엔드포인트
     * 
     * @param clientId 연결을 요청한 클라이언트의 고유 식별자
     * @return SSE 연결을 위한 이미터 객체
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestParam String clientId) {
        return sseEmitterService.createEmitter(clientId);
    }

    /**
     * 특정 클라이언트에게 이벤트를 전송하는 엔드포인트
     * 
     * @param clientId 대상 클라이언트 ID
     * @param event 전송할 이벤트 데이터
     */
    @PostMapping("/send/{clientId}")
    public void sendToClient(@PathVariable String clientId, @RequestBody SseEvent event) {
        sseEmitterService.sendToClient(clientId, event);
    }

    /**
     * 모든 연결된 클라이언트에게 이벤트를 브로드캐스트하는 엔드포인트
     * 
     * @param event 브로드캐스트할 이벤트 데이터
     */
    @PostMapping("/broadcast")
    public void broadcast(@RequestBody SseEvent event) {
        sseEmitterService.broadcast(event);
    }
}