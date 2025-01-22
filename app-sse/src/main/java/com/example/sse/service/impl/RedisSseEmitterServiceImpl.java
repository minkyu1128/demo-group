package com.example.sse.service.impl;

import com.example.sse.domain.EventProcessor;
import com.example.sse.domain.ReconnectionHandler;
import com.example.sse.domain.SseConnectionManager;
import com.example.sse.model.RedisMessage;
import com.example.sse.model.SseEvent;
import com.example.sse.service.SseEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSseEmitterServiceImpl implements SseEmitterService {
    private final Logger logger = LoggerFactory.getLogger(RedisSseEmitterServiceImpl.class);

    private static final String REDIS_SSE_CHANNEL = "sse:events";
    private static final String REDIS_SSE_LASTEVENT = "sse:lastEvent:events";

    private final SseConnectionManager connectionManager;
    private final EventProcessor eventProcessor;
    private final ReconnectionHandler reconnectionHandler;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer redisMessageListener;
    private final ExecutorService executorService = Executors.newCachedThreadPool();


    public RedisSseEmitterServiceImpl(SseConnectionManager connectionManager, EventProcessor eventProcessor, ReconnectionHandler reconnectionHandler, RedisTemplate<String, Object> redisTemplate, RedisMessageListenerContainer redisMessageListener) {
        this.connectionManager = connectionManager;
        this.eventProcessor = eventProcessor;
        this.reconnectionHandler = reconnectionHandler;
        this.redisTemplate = redisTemplate;
        this.redisMessageListener = redisMessageListener;
        subscribeToRedisChannel();
    }

    //이벤트 유실에 대한 처리
    //case1. Publish 실패한 Client Event만 Redis에 저장
    //  -> 브로드캐스트: 첫번째 실패 이후 클라이언트를 삭제하기에 두번째 이벤트부터는 저장하지 않음
    //  -> 클라이언트에게 전송: 실패 시 Redis 에 저장
    //case2. Publish 하기 전 Redis 에 Event 저장
    //  -> 브로드 캐스트: case1과 동일하나, 모든 유저의 이벤트를 저장하기 메모리 부담이 있음
    //  -> 클라이언트에게 전송: 성공/실패 관계 없이 Redis 에 저장하기 때문에 메모리 부담이 있음

    // --> Event유형_타임스탬프 형식으로 저장소에 저장

    /**
     * Redis 채널 구독 설정
     */
    private void subscribeToRedisChannel() {
        final MessageListener messageListener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    RedisMessage redisMessage = (RedisMessage) redisTemplate.getValueSerializer()
                            .deserialize(message.getBody());
                    handleRedisMessage(redisMessage);
                } catch (Exception e) {
                    logger.error("Redis message processing failed", e);
                }
            }
        };
        final Topic channelTopic = new ChannelTopic(REDIS_SSE_CHANNEL); //구독(subscribe) 채널 설정
        redisMessageListener.addMessageListener(messageListener, channelTopic);
    }


    @Override
    public SseEmitter createEmitter(String clientId, String lastEventId) {
        // 1. 저장소에 클라이언트 연결 추가
        SseEmitter emitter = connectionManager.createEmitter(clientId);

        // 2. 초기 연결 메시지 전송
        connectionManager.sendInitialConnection(clientId, emitter);

        // 3. 재연결 처리
        if (lastEventId != null) {
            handleReconnection(emitter, clientId, lastEventId);
        }

        return emitter;
    }

    /**
     * 재연결 처리
     *
     * @param emitter
     * @param clientId
     * @param lastEventId
     */
    private void handleReconnection(SseEmitter emitter, String clientId, String lastEventId) {
        try {
            //놓친 이벤트 목록 조회
            Set<String> eventKeys = reconnectionHandler.getStoredEventKeys(REDIS_SSE_LASTEVENT);
            List<SseEvent> missedEvents = reconnectionHandler.getMissedEvents(eventKeys, lastEventId);
            //놓친 이벤트 재전송
            for (SseEvent event : missedEvents) {
                eventProcessor.handleEvent(emitter, clientId, event);
            }
        } catch (Exception e) {
            logger.error("Reconnection failed for client: {}", clientId, e);
            connectionManager.removeClient(clientId);
        }
    }


    @Override
    public void sendToClient(String clientId, SseEvent event) {
        RedisMessage message = new RedisMessage("SINGLE", clientId, event);
        redisTemplate.convertAndSend(REDIS_SSE_CHANNEL, message);
    }


    @Override
    public void broadcast(SseEvent event) {
        RedisMessage message = new RedisMessage("BROADCAST", null, event);
        redisTemplate.convertAndSend(REDIS_SSE_CHANNEL, message);
    }

    @Override
    public int getConnectedClientCount() {
        // Redis Set에 저장된 전체 클라이언트 수 조회
        Long totalCount = connectionManager.getTotalClients();
        return totalCount != null ? totalCount.intValue() : 0;
    }

    @Override
    public void closeConnection(String clientId) {
        RedisMessage message = new RedisMessage("CLOSE", clientId, null);
        redisTemplate.convertAndSend(REDIS_SSE_CHANNEL, message);
    }

    @Override
    public void shutdown() {
        RedisMessage message = new RedisMessage("SHUTDOWN", null, null);
        redisTemplate.convertAndSend(REDIS_SSE_CHANNEL, message);

    }

    /**
     * Redis 메시지 처리
     */
    private void handleRedisMessage(RedisMessage message) {
        // 이벤트를 Redis에 저장 (재연결을 위해)
        final String eventId = message.getEvent().getEvent() + "_" + System.currentTimeMillis();
        message.getEvent().setId(eventId);
        String eventKey = REDIS_SSE_LASTEVENT + ":" + eventId;
        reconnectionHandler.storeEvent(message.getEvent(), eventKey);
        //메시지 유형에 따라 전송
        if ("SINGLE".equals(message.getType())) {
            sendToLocalClient(message.getClientId(), message.getEvent());
        } else if ("BROADCAST".equals(message.getType())) {
            broadcastToLocalClients(message.getEvent());
        } else if ("CLOSE".equals(message.getType())) {
            connectionManager.close(message.getClientId());
        } else if ("SHUTDOWN".equals(message.getType())) {
            connectionManager.shutdown();
            // ExecutorService 종료
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            // Redis 리스너 제거
            redisMessageListener.removeMessageListener(
                    (msg, pattern) -> {
                    },
                    new ChannelTopic(REDIS_SSE_CHANNEL)
            );
        }
    }


    /**
     * 로컬 클라이언트에게 이벤트 전송
     */
    private void sendToLocalClient(String clientId, SseEvent event) {
        SseEmitter emitter = connectionManager.getLocalEmitter(clientId);
        if (emitter != null) {
//            try {
            executorService.execute(() -> {
                try {
                    eventProcessor.handleEvent(emitter, clientId, event);
                } catch (Exception e) {
                    logger.error("Failed to send event to client: {}", clientId, e);
                    connectionManager.removeClient(clientId);
                }
            });
//            } catch (Exception e) {
//                logger.error("Failed to execute send task for client: {}", clientId, e);
//                connectionManager.removeClient(clientId);
//            }
        }
    }

    /**
     * 로컬에 연결된 모든 클라이언트에게 이벤트 전송
     */
    private void broadcastToLocalClients(SseEvent event) {
        connectionManager.getLocalEmitterALL().forEach((clientId, emitter) ->
                sendToLocalClient(clientId, event)
        );
    }


}