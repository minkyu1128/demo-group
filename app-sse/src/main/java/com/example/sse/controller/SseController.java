package com.example.sse.controller;

import com.example.sse.model.SseEvent;
import com.example.sse.service.SseEmitterService;
import com.example.sse.service.impl.BasicSseEmitterServiceImpl;
import com.example.sse.service.impl.RedisSseEmitterServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE(Server-Sent Events) 관련 엔드포인트를 제공하는 REST 컨트롤러
 * 클라이언트의 SSE 연결 요청 처리 및 이벤트 전송을 담당
 */
@RestController
@RequestMapping(value = {"/api/v1/sse", "/api/v1/sse-{messageBroker}"})
public class SseController {

    private final SseEmitterService basicSseEmitterService; //단일 서버
    private final SseEmitterService redisSseEmitterService; //이중화 서버(with Redis)

    public SseController(BasicSseEmitterServiceImpl basicSseEmitterService, RedisSseEmitterServiceImpl redisSseEmitterService) {
        this.basicSseEmitterService = basicSseEmitterService;
        this.redisSseEmitterService = redisSseEmitterService;
    }

    private SseEmitterService getSseEmitterService(String messageBroker) {
        if ("redis".equals(messageBroker)) return redisSseEmitterService;
        else return basicSseEmitterService;
    }

    /**
     * 클라이언트의 SSE 연결 요청을 처리하는 엔드포인트
     *
     * @param clientId 연결을 요청한 클라이언트의 고유 식별자
     * @return SSE 연결을 위한 이미터 객체
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable(required = false) String messageBroker, @RequestParam String clientId) {
        return getSseEmitterService(messageBroker).createEmitter(clientId);
    }

    /**
     * 특정 클라이언트에게 이벤트를 전송하는 엔드포인트
     *
     * @param clientId 대상 클라이언트 ID
     * @param event    전송할 이벤트 데이터
     */
    @PostMapping("/send/{clientId}")
    public void sendToClient(@PathVariable(required = false) String messageBroker, @PathVariable String clientId, @RequestBody SseEvent event) {
        getSseEmitterService(messageBroker).sendToClient(clientId, event);

    }

    /**
     * 모든 연결된 클라이언트에게 이벤트를 브로드캐스트하는 엔드포인트
     *
     * @param event 브로드캐스트할 이벤트 데이터
     */
    @PostMapping("/broadcast")
    public void broadcast(@PathVariable(required = false) String messageBroker, @RequestBody SseEvent event) {
        getSseEmitterService(messageBroker).broadcast(event);
    }

    /**
     * 현재 연결된 클라이언트 수를 조회하는 엔드포인트
     *
     * @return 연결된 클라이언트 수
     */
    @GetMapping("/connected-count")
    public int getConnectedCount(@PathVariable(required = false) String messageBroker) {
        return getSseEmitterService(messageBroker).getConnectedClientCount();
    }

    /**
     * 특정 클라이언트의 SSE 연결을 종료하는 엔드포인트
     *
     * @param clientId 종료할 클라이언트의 ID
     */
    @PostMapping("/disconnect/{clientId}")
    public void disconnect(@PathVariable(required = false) String messageBroker, @PathVariable String clientId) {
        getSseEmitterService(messageBroker).closeConnection(clientId);
    }

    /**
     * 모든 SSE 연결을 종료하고 서비스를 정리하는 엔드포인트
     */
    @PostMapping("/shutdown")
    public void shutdown(@PathVariable(required = false) String messageBroker) {
        getSseEmitterService(messageBroker).shutdown();
    }
}