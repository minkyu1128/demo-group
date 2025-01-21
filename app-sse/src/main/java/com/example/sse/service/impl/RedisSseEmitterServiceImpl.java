package com.example.sse.service.impl;

import com.example.sse.model.RedisMessage;
import com.example.sse.model.SseEvent;
import com.example.sse.service.SseEmitterService;
import com.example.sse.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisSseEmitterServiceImpl implements SseEmitterService {
    private static final String REDIS_SSE_CHANNEL = "sse:events";
    private static final String REDIS_SSE_CLIENTS = "sse:clients";
    private static final String REDIS_SSE_LASTEVENT = "sse:lastEvent:events";

    private final ConcurrentHashMap<String, SseEmitter> localEmitters = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, Object> lastEventTemplate;
    private final RedisMessageListenerContainer redisMessageListener;
    private final Logger logger = LoggerFactory.getLogger(RedisSseEmitterServiceImpl.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private final long reconnectTimeMillis = 10 * 1000L;

    public RedisSseEmitterServiceImpl(RedisTemplate<String, Object> redisTemplate, RedisTemplate<String, Object> lastEventTemplate, RedisMessageListenerContainer redisMessageListener) {
        this.redisTemplate = redisTemplate;
        this.lastEventTemplate = lastEventTemplate;
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
    public SseEmitter createEmitter(String clientId) {
        SseEmitter emitter = new SseEmitter(Duration.ofMinutes(30).toMillis());    // 타임아웃 시간 설정

        emitter.onCompletion(() -> removeClient(clientId));
        emitter.onTimeout(() -> removeClient(clientId));
        emitter.onError(e -> removeClient(clientId));

        localEmitters.put(clientId, emitter);
        redisTemplate.opsForSet().add(REDIS_SSE_CLIENTS, clientId);
        // 연결 완료 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .id(clientId + "_" + System.currentTimeMillis())           // 이벤트 ID
                    .name("connect")      // 이벤트 이름
                    .data("{\"result\": \"success\"}", MediaType.APPLICATION_JSON)  // 이벤트 데이터
                    .reconnectTime(reconnectTimeMillis) // 재연결 시간
                    .comment("Duplex Server System - Connected")       // 주석
            );

            // 연결 시 마지막 이벤트 이후의 메시지 재전송
            String lastEventId = RequestUtils.getCurrentHttpRequest().getHeader("Last-Event-ID");
            if (lastEventId != null) {
                sendMissedEvents(emitter, clientId, lastEventId);
            }
        } catch (Exception e) {
            logger.error("Failed to send initial connection message", e);
            removeClient(clientId);
            return null;
        }

        return emitter;
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
        Long totalCount = redisTemplate.opsForSet().size(REDIS_SSE_CLIENTS);
        return totalCount != null ? totalCount.intValue() : 0;
    }

    @Override
    public void closeConnection(String clientId) {
        SseEmitter emitter = localEmitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.complete();  // 연결 정상 종료
            } catch (Exception e) {
                logger.error("Error while closing connection for client: {}", clientId, e);
            } finally {
                removeClient(clientId);  // Redis에서도 제거
                logger.info("Connection closed for client: {}", clientId);
            }
        }
    }

    @Override
    public void shutdown() {
        // 로컬에 연결된 모든 클라이언트의 연결을 정상적으로 종료
        localEmitters.forEach((clientId, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception e) {
                logger.error("Error while shutting down connection for client: {}", clientId, e);
            }
        });

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
                (message, pattern) -> {
                },
                new ChannelTopic(REDIS_SSE_CHANNEL)
        );

        // 로컬 저장소 초기화
        localEmitters.clear();
        logger.info("SSE service has been shut down");
    }

    /**
     * Redis 메시지 처리
     */
    private void handleRedisMessage(RedisMessage message) {
        // 이벤트를 Redis에 저장 (재연결을 위해)
        final String eventId = message.getEvent().getEvent() + "_" + System.currentTimeMillis();
        message.getEvent().setId(eventId);
        storeEventInRedis(message.getEvent(), eventId);
        //메시지 유형에 따라 전송
        if ("SINGLE".equals(message.getType())) {
            sendToLocalClient(message.getClientId(), message.getEvent());
        } else if ("BROADCAST".equals(message.getType())) {
            broadcastToLocalClients(message.getEvent());
        }
    }


    /**
     * 로컬 클라이언트에게 이벤트 전송
     */
    private void sendToLocalClient(String clientId, SseEvent event) {
        SseEmitter emitter = localEmitters.get(clientId);
        if (emitter != null) {
            try {
                executorService.execute(() -> {
                    try {
                        // 이벤트 전송
                        emitter.send(SseEmitter.event()
                                .id(event.getId())
                                .name(event.getEvent())
                                .data(event.getData(), MediaType.APPLICATION_JSON)
                                .reconnectTime(event.getRetry() != null ? event.getRetry() : reconnectTimeMillis));
                    } catch (Exception e) {
                        logger.error("Failed to send event to client: {}", clientId, e);
                        removeClient(clientId);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to execute send task for client: {}", clientId, e);
                removeClient(clientId);
            }
        }
    }

    /**
     * 로컬에 연결된 모든 클라이언트에게 이벤트 전송
     */
    private void broadcastToLocalClients(SseEvent event) {
        localEmitters.forEach((clientId, emitter) ->
                sendToLocalClient(clientId, event)
        );
    }

    /**
     * 클라이언트 제거
     */
    private void removeClient(String clientId) {
        localEmitters.remove(clientId);
        redisTemplate.opsForSet().remove(REDIS_SSE_CLIENTS, clientId);
        logger.info("Client removed: {}", clientId);
    }

    /**
     * Redis에 이벤트 저장을 위한 메서드 추가
     */
    private void storeEventInRedis(SseEvent event, String eventID) {
        String eventKey = REDIS_SSE_LASTEVENT + ":" + eventID;
        lastEventTemplate.opsForValue().set(eventKey, event, Duration.ofMinutes(15));  // 15분 동안 유지
    }
    /**
     * 놓친 이벤트 재전송
     */
    private void sendMissedEvents(SseEmitter emitter, String clientId, String lastEventId) {
        try {
            // 마지막 이벤트 시간 추출
            long lastEventTime = extractEventTime(lastEventId);
            String eventsPattern = REDIS_SSE_LASTEVENT + ":*";

            // Redis에서 이벤트 히스토리 조회
            Set<String> eventKeys = redisTemplate.keys(eventsPattern);
            if (eventKeys != null && !eventKeys.isEmpty()) {
                List<SseEvent> missedEvents = eventKeys.stream()
                        .map(key -> (SseEvent) lastEventTemplate.opsForValue().get(key))
                        .filter(event -> event != null && extractEventTime(event.getId()) > lastEventTime)
                        .sorted(Comparator.comparingLong(event -> extractEventTime(event.getId())))
                        .collect(Collectors.toList());

                // 놓친 이벤트 재전송
                for (SseEvent event : missedEvents) {
                    try {
                        emitter.send(SseEmitter.event()
                                .id(event.getId())
                                .name(event.getEvent())
                                .data(event.getData(), MediaType.APPLICATION_JSON)
                                .reconnectTime(reconnectTimeMillis));

                        // 재전송 로깅
                        logger.info("Resent missed event {} to client {}", event.getId(), clientId);
                    } catch (Exception e) {
                        logger.error("Failed to resend missed event {} to client {}", event.getId(), clientId, e);
                        throw e;
                    }
                }

                logger.info("Resent {} missed events to client {}", missedEvents.size(), clientId);
            }
        } catch (Exception e) {
            logger.error("Error processing missed events for client {}", clientId, e);
            removeClient(clientId);
        }
    }

    /**
     * 이벤트 ID에서 이벤트 시간 추출
     */
    private long extractEventTime(String eventId) {
        try {
            // eventId 형식: clientId_timestamp 또는 timestamp
            String[] parts = eventId.split("_");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            logger.error("Failed to parse event time from ID: {}", eventId);
            return 0L;
        }
    }

}