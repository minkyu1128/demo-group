package com.example.sse.service;

import com.example.sse.model.SseEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {
    /**
     * 새로운 SSE 연결을 생성하고 관리하는 메서드
     *
     * @param clientId 클라이언트 식별자
     * @return 생성된 SSE 이미터
     * @throws IllegalArgumentException 클라이언트 ID가 유효하지 않은 경우
     */
    SseEmitter createEmitter(String clientId, String lastEventId);

    /**
     * 특정 클라이언트에게 이벤트를 전송하는 메서드
     *
     * @param clientId 대상 클라이언트 ID
     * @param event    전송할 이벤트 데이터
     * @throws RuntimeException 클라이언트를 찾을 수 없거나 전송 실패 시
     */
    void sendToClient(String clientId, SseEvent event);

    /**
     * 모든 연결된 클라이언트에게 이벤트를 브로드캐스트하는 메서드
     *
     * @param event 브로드캐스트할 이벤트 데이터
     */
    void broadcast(SseEvent event);

    /**
     * 현재 연결된 클라이언트 수를 반환하는 메서드
     *
     * @return 연결된 클라이언트 수
     */
    int getConnectedClientCount();

    /**
     * 특정 클라이언트의 연결을 종료하는 메서드
     *
     * @param clientId 종료할 클라이언트 ID
     */
    void closeConnection(String clientId);

    /**
     * 서비스 종료 시 정리 작업을 수행하는 메서드
     */
    void shutdown();
}
