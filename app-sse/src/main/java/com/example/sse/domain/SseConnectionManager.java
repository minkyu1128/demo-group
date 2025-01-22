package com.example.sse.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * SSE(Server-Sent Events) 연결을 관리하는 클래스
 */
@Component
public class SseConnectionManager {
    private final Logger logger = LoggerFactory.getLogger(SseConnectionManager.class);

    private static final String REDIS_SSE_CLIENTS = "sse:clients";
    private final ConcurrentHashMap<String, SseEmitter> localEmitters = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public SseConnectionManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());    // 타임아웃 시간 설정
        emitter.onCompletion(() -> removeClient(clientId));
        emitter.onTimeout(() -> removeClient(clientId));
        emitter.onError(e -> removeClient(clientId));

        //
        registerClient(clientId, emitter);

        return emitter;
    }

    public void sendInitialConnection(String clientId, SseEmitter emitter) {

        // 연결 완료 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .id(clientId + "_" + System.currentTimeMillis())           // 이벤트 ID
                    .name("connect")      // 이벤트 이름
                    .data("{\"result\": \"success\"}", MediaType.APPLICATION_JSON)  // 이벤트 데이터
                    .reconnectTime(10 * 1000L) // 재연결 시간
                    .comment("Duplex Server System - Connected")       // 주석
            );
        } catch (Exception e) {
            logger.error("Failed to send initial connection message", e);
            removeClient(clientId);
        }
    }

    public void registerClient(String clientId, SseEmitter emitter) {
        localEmitters.put(clientId, emitter);
        redisTemplate.opsForSet().add(REDIS_SSE_CLIENTS, clientId);
    }

    public Long getTotalClients() {
        return redisTemplate.opsForSet().size(REDIS_SSE_CLIENTS);
    }


    public SseEmitter getLocalEmitter(String clientId) {
        return localEmitters.get(clientId);
    }

    public ConcurrentHashMap<String, SseEmitter> getLocalEmitterALL() {
        return localEmitters;
    }

    /**
     * 클라이언트 제거
     */
    public void removeClient(String clientId) {
        localEmitters.remove(clientId);
        redisTemplate.opsForSet().remove(REDIS_SSE_CLIENTS, clientId);
        logger.info("Client removed: {}", clientId);
    }

    public void close(String clientId) {
        SseEmitter emitter = localEmitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.complete();  // 연결 정상 종료
            } catch (Exception e) {
                logger.error("Error while closing connection for client: {}", clientId, e);
            } finally {
                removeClient(clientId);
                logger.info("Connection closed for client: {}", clientId);
            }
        }
    }

    public void shutdown() {
        // 로컬에 연결된 모든 클라이언트의 연결을 정상적으로 종료
        localEmitters.forEach((clientId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.error("Error while shutting down connection for client: {}", clientId, e);
            }
        });
        // 로컬 저장소 초기화
        localEmitters.clear();
        logger.info("SSE service has been shut down");
    }
}
