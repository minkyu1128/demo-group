package com.example.sse.domain;

import com.example.sse.model.SseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 이벤트 처리기
 */
@Component
public class EventProcessor {
    private final Logger logger = LoggerFactory.getLogger(ReconnectionHandler.class);

    public void handleEvent(SseEmitter emitter, String clientId, SseEvent event) {
        try {
            emitter.send(SseEmitter.event()
                    .id(event.getId())
                    .name(event.getEvent())
                    .data(event.getData(), MediaType.APPLICATION_JSON)
                    .reconnectTime(event.getRetry() != null ? event.getRetry() : 10 * 1000L));
        } catch (IOException e) {
//            throw new SseEventProcessingException("Failed to send event", e);
            throw new RuntimeException("Failed to send event for Client: {" + clientId + "}", e);

        }
    }
}
