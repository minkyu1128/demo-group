package com.example.sse.domain;

import com.example.sse.model.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 재연결을 처리하는 클래스
 */
@Component
public class ReconnectionHandler {
    private final Logger logger = LoggerFactory.getLogger(ReconnectionHandler.class);

    private final RedisTemplate<String, Object> lastEventTemplate;

    public ReconnectionHandler(RedisTemplate<String, Object> lastEventTemplate) {
        this.lastEventTemplate = lastEventTemplate;
    }

    /**
     * 이벤트를 저장 합니다 *
     *
     * @param event
     */
    public void storeEvent(SseEvent event, String eventKey) {
        lastEventTemplate.opsForValue().set(eventKey, event, Duration.ofMinutes(15));
    }

    /**
     * 저장된 이벤트 키 목록을 반환합니다
     *
     * @param eventKey
     * @return
     */
    public Set<String> getStoredEventKeys(String eventKey) {
        return lastEventTemplate.keys(eventKey + "*");
    }

    /**
     * Last-Event-ID 이후 발생한 이벤트를 반환합니다
     *
     * @return
     */
    public List<SseEvent> getMissedEvents(Set<String> eventKeys, String lastEventId) {
        long lastEventTime = extractEventTime(lastEventId);
        return eventKeys.stream()
                .map(key -> (SseEvent) lastEventTemplate.opsForValue().get(key))
                .filter(event -> event != null && extractEventTime(event.getId()) > lastEventTime)
                .sorted(Comparator.comparingLong(event -> extractEventTime(event.getId())))
                .collect(Collectors.toList());
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
