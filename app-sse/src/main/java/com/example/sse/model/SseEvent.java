package com.example.sse.model;

import java.util.Map;

/**
 * SSE 이벤트 데이터를 표현하는 모델 클래스
 * SSE 프로토콜에서 정의하는 이벤트 구조를 Java 객체로 표현
 */
public class SseEvent {
    /**
     * 이벤트의 고유 식별자
     */
    private String id;
    /**
     * 이벤트의 타입 또는 이름
     */
    private String event;
    /**
     * 이벤트에 포함될 실제 데이터
     */
//    private String data;
    private Map<String, Object> data;
    /**
     * 연결 재시도 간격 (밀리초 단위)
     */
    private Long retry;

    /**
     * 기본 생성자
     */
    public SseEvent() {
    }

    /**
     * 모든 필드를 초기화하는 생성자
     *
     * @param id    이벤트 ID
     * @param event 이벤트 이름
     * @param data  이벤트 데이터
     * @param retry 재연결 시도 간격
     */
//    public SseEvent(String id, String event, String data, Long retry) {
    public SseEvent(String id, String event, Map<String, Object> data, Long retry) {
        this.id = id;
        this.event = event;
        this.data = data;
        this.retry = retry;
    }

    // Getter와 Setter 메서드
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    //    public String getData() {
    public Map<String, Object> getData() {
        return data;
    }

    //    public void setData(String data) {
    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Long getRetry() {
        return retry;
    }

    public void setRetry(Long retry) {
        this.retry = retry;
    }
}