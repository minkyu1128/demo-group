package com.example.sse.service.impl;

import com.example.sse.model.SseEvent;
import com.example.sse.service.SseEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Sent Events(SSE) 처리를 위한 서비스 클래스
 * 클라이언트와의 SSE 연결을 관리하고 이벤트를 전송하는 기능을 제공
 */
@Service
public class BasicSseEmitterServiceImpl implements SseEmitterService {
    // 클라이언트 ID를 키로 하는 SSE 이미터 저장소
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(BasicSseEmitterServiceImpl.class);
    private final long reconnectTimeMillis = 10 * 1000L;

    @Override
    public SseEmitter createEmitter(String clientId, String lastEventId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMillis(30).toMillis());  // 타임아웃 시간 설정

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

        // 연결 완료 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                            .id(clientId + "_" + System.currentTimeMillis())           // 이벤트 ID
                            .name("connect")      // 이벤트 이름
//                    .data("connected Success!")  // 이벤트 데이터
                            .data("{\"result\": \"success\"}", MediaType.APPLICATION_JSON)  // 이벤트 데이터
                            .reconnectTime(reconnectTimeMillis) // 재연결 시간
                            .comment("Single Server SSE - Connected")       // 주석
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect send event to client: " + clientId, e);
        }

        // 이미터를 저장소에 저장
        emitters.put(clientId, emitter);
        return emitter;
    }

    @Override
    public void sendToClient(String clientId, SseEvent event) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter == null) {
            throw new RuntimeException("Client not found: " + clientId);
        }

        try {
            // 이벤트 전송 설정 및 실행
            emitter.send(SseEmitter.event()
//                    .id(event.getId())           // 이벤트 ID
                    .id(event.getEvent() + "_" + System.currentTimeMillis())           // 이벤트 ID
                    .name(event.getEvent())      // 이벤트 이름
                    .data(event.getData())       // 이벤트 데이터
                    .reconnectTime(event.getRetry() != null ? event.getRetry() : reconnectTimeMillis)); // 재연결 시간
        } catch (Exception e) {
            emitters.remove(clientId);
            throw new RuntimeException("Failed to send event to client: " + clientId, e);
        }
    }

    @Override
    public void broadcast(SseEvent event) {
        emitters.forEach((clientId, emitter) -> {
            try {
                sendToClient(clientId, event);
            } catch (Exception e) {
                logger.error("Failed to broadcast event to client: {}", clientId, e);
            }
        });
    }

    @Override
    public int getConnectedClientCount() {
        return emitters.size();
    }

    @Override
    public void closeConnection(String clientId) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.complete();  // 연결 정상 종료
            } catch (Exception e) {
                logger.error("Error while closing connection for client: {}", clientId, e);
            } finally {
                emitters.remove(clientId);
                logger.info("Connection closed for client: {}", clientId);
            }
        }
    }

    @Override
    public void shutdown() {
        // 모든 연결된 클라이언트의 연결을 정상적으로 종료
        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.error("Error while shutting down connection for client: {}", clientId, e);
            }
        });

        // 저장소 초기화
        emitters.clear();
        logger.info("All SSE connections have been shut down");
    }
}