package com.example.sse.service;

import com.example.sse.model.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Sent Events(SSE) 처리를 위한 서비스 클래스
 * 클라이언트와의 SSE 연결을 관리하고 이벤트를 전송하는 기능을 제공
 */
@Service
public class SseEmitterService {
    // 클라이언트 ID를 키로 하는 SSE 이미터 저장소
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(SseEmitterService.class);

    /**
     * 새로운 SSE 연결을 생성하고 관리하는 메서드
     * 
     * @param clientId 클라이언트 식별자
     * @return 생성된 SSE 이미터
     */
    public SseEmitter createEmitter(String clientId) {
        // 1시간 타임아웃으로 새로운 SSE 이미터 생성
        SseEmitter emitter = new SseEmitter(Duration.ofHours(1).toMillis());
        
        // 연결이 완료될 때 실행되는 콜백
        emitter.onCompletion(() -> {
            emitters.remove(clientId);
            logger.info("SSE connection completed for client: {}", clientId);
        });
        
        // 타임아웃 발생 시 실행되는 콜백
        emitter.onTimeout(() -> {
            emitters.remove(clientId);
            logger.info("SSE connection timed out for client: {}", clientId);
        });
        
        // 에러 발생 시 실행되는 콜백
        emitter.onError(e -> {
            emitters.remove(clientId);
            logger.error("SSE error occurred for client: {}", clientId, e);
        });
        
        // 이미터를 저장소에 저장
        emitters.put(clientId, emitter);
        return emitter;
    }

    /**
     * 특정 클라이언트에게 이벤트를 전송하는 메서드
     * 
     * @param clientId 대상 클라이언트 ID
     * @param event 전송할 이벤트 데이터
     * @throws RuntimeException 클라이언트를 찾을 수 없거나 전송 실패 시
     */
    public void sendToClient(String clientId, SseEvent event) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            throw new RuntimeException("Client not found: " + clientId);
        }
        
        try {
            // 이벤트 전송 설정 및 실행
            emitter.send(SseEmitter.event()
                    .id(event.getId())           // 이벤트 ID
                    .name(event.getEvent())      // 이벤트 이름
                    .data(event.getData())       // 이벤트 데이터
                    .reconnectTime(event.getRetry() != null ? event.getRetry() : 10000L)); // 재연결 시간
        } catch (Exception e) {
            emitters.remove(clientId);
            throw new RuntimeException("Failed to send event to client: " + clientId, e);
        }
    }

    /**
     * 모든 연결된 클라이언트에게 이벤트를 브로드캐스트하는 메서드
     * 
     * @param event 브로드캐스트할 이벤트 데이터
     */
    public void broadcast(SseEvent event) {
        emitters.forEach((clientId, emitter) -> {
            try {
                sendToClient(clientId, event);
            } catch (Exception e) {
                logger.error("Failed to broadcast event to client: {}", clientId, e);
            }
        });
    }
}