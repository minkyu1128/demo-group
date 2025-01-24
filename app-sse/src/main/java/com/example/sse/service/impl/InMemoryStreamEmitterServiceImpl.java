package com.example.sse.service.impl;

import com.example.sse.domain.EventProcessor;
import com.example.sse.domain.SseConnectionManager;
import com.example.sse.model.SseEvent;
import com.example.sse.service.SseEmitterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

@Service
public class InMemoryStreamEmitterServiceImpl implements SseEmitterService {
    private final Logger logger = LoggerFactory.getLogger(InMemoryStreamEmitterServiceImpl.class);

    // 메시지 저장소: streamKey -> List<Event>
    private final ConcurrentHashMap<String, List<SseEvent>> messageStore = new ConcurrentHashMap<>();

    // Consumer Group 관리: groupId -> Set<ConsumerId>
    private final ConcurrentHashMap<String, Set<String>> consumerGroups = new ConcurrentHashMap<>();

    // Consumer Offset 관리: consumerId -> lastEventId
    private final ConcurrentHashMap<String, String> consumerOffsets = new ConcurrentHashMap<>();

    // 순서가 보장되는 메시지 큐 추가
    private final ConcurrentHashMap<String, BlockingQueue<SseEvent>> messageQueues = new ConcurrentHashMap<>();

    // 메시지 처리를 위한 전용 스레드 풀
    private final ExecutorService messageProcessorExecutor = Executors.newSingleThreadExecutor();

    private final SseConnectionManager connectionManager;
    private final EventProcessor eventProcessor;
    private final ExecutorService executorService;

    private static final String DEFAULT_GROUP = "default-group";
    private static final String STREAM_PREFIX = "stream:";
    private static final int MESSAGE_RETENTION_SIZE = 1000; // 스트림당 보관할 최대 메시지 수

    public InMemoryStreamEmitterServiceImpl(SseConnectionManager connectionManager, EventProcessor eventProcessor) {
        this.connectionManager = connectionManager;
        this.eventProcessor = eventProcessor;
        this.executorService = Executors.newCachedThreadPool();
        startMessageProcessor();
    }


    //[Redis Streams]
    //1. 하나의 스트림에 모든 메시지가 append
    //2. Consumer Group이 메시지의 소비 상태를 관리
    //3. 각 Consumer가 서로 다른 메시지를 처리 (분산 처리)
    //4. ACK를 통한 메시지 처리 확인

    //[현재 구현]
    //1. 클라이언트별 독립적인 큐
    //2. 큐에서 poll()하면 메시지가 즉시 제거
    //3. 단일 프로세서가 모든 큐를 처리


    /**
     * 메시지 처리 스레드 시작
     */
    private void startMessageProcessor() {
        messageProcessorExecutor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("processMessageQueues...");
                try {
                    processMessageQueues();
                    Thread.sleep(500); // 폴링 간격
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * 메시지 큐 처리
     */
    private void processMessageQueues() {
        messageQueues.forEach((streamKey, queue) -> {
            SseEvent event = queue.poll();
            if (event != null) {
                // 메시지 저장소에 저장 (히스토리용)
                storeMessage(streamKey, event);

                // Consumer Group의 구독자들에게 전달
                if (streamKey.contains("broadcast")) {
                    deliverBroadcastMessage(event);
                } else {
                    String clientId = streamKey.replace(STREAM_PREFIX, "");
                    deliverMessageToClient(clientId, event);
                }
            }
        });
    }

    @Override
    public SseEmitter createEmitter(String clientId, String lastEventId) {
        // 1. Emitter 생성
        SseEmitter emitter = connectionManager.createEmitter(clientId);

        // 2. Consumer Group에 클라이언트 추가
        consumerGroups.computeIfAbsent(DEFAULT_GROUP, k -> ConcurrentHashMap.newKeySet())
                .add(clientId);

        // 3. 초기 연결 메시지 전송
        connectionManager.sendInitialConnection(clientId, emitter);

        // 4. Last Event ID가 있는 경우 누락된 메시지 재전송
        if (lastEventId != null) {
            replayMessages(clientId, lastEventId, emitter);
        }

        return emitter;
    }

    @Override
    public void sendToClient(String clientId, SseEvent event) {
        generateEventID(event);
        String streamKey = STREAM_PREFIX + clientId;

        // 메시지 큐에 추가
        messageQueues.computeIfAbsent(streamKey, k -> new LinkedBlockingQueue<>(MESSAGE_RETENTION_SIZE))
                .offer(event);
    }

    @Override
    public void broadcast(SseEvent event) {
        generateEventID(event);
        String streamKey = STREAM_PREFIX + "broadcast";

        // 브로드캐스트 큐에 추가
        messageQueues.computeIfAbsent(streamKey, k -> new LinkedBlockingQueue<>(MESSAGE_RETENTION_SIZE))    //Thread-Safe 한 computeIfAbsent 사용. putIfAbsent 사용 시 구현 패턴에 따라 레이스 컨디션 발생. 아래 주석 참고
                .offer(event);
        /**
         * [ 안티 패턴 ]
         * if (!messageQueues.containsKey("key")) {     // 체크
         *     Queue<SseEvent> queue = new LinkedBlockingQueue<>();
         *     queues.putIfAbsent("key", queue);        // 삽입
         *     queue.offer(event);                      // 사용
         * }
         *
         * // 위 코드에서 레이스 컨디션 시나리오:
         * Thread 1: if (!messageQueues.containsKey("key"))  // false
         * Thread 2: if (!messageQueues.containsKey("key"))  // false
         * Thread 1: new LinkedBlockingQueue()        // queue1 생성
         * Thread 2: new LinkedBlockingQueue()        // queue2 생성
         * Thread 1: putIfAbsent("key", queue1)      // 성공
         * Thread 2: putIfAbsent("key", queue2)      // 실패하지만 queue2에 이벤트 추가
         * Thread 2: messageQueues.offer(event)      // 버려지는 큐에 이벤트 추가
         */
    }

    @Override
    public int getConnectedClientCount() {
        return connectionManager.getTotalClients().intValue();
    }

    @Override
    public void closeConnection(String clientId) {
        // Consumer Group에서 제거
        consumerGroups.get(DEFAULT_GROUP).remove(clientId);
        consumerOffsets.remove(clientId);
        connectionManager.close(clientId);
    }

    @Override
    public void shutdown() {
        messageProcessorExecutor.shutdown();
        executorService.shutdown();
        try {
            if (!messageProcessorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                messageProcessorExecutor.shutdownNow();
            }
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            messageProcessorExecutor.shutdownNow();
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        connectionManager.shutdown();
    }

    private void generateEventID(SseEvent event) {
        final String eventId = event.getEvent() + "_" + System.currentTimeMillis();
        event.setId(eventId);
    }


    /**
     * 메시지 저장소에 메시지 저장
     *
     * @param streamKey
     * @param event
     */
    private void storeMessage(String streamKey, SseEvent event) {
        messageStore.computeIfAbsent(streamKey, k -> new CopyOnWriteArrayList<>())
                .add(event);

        // 메시지 수 제한 관리
        List<SseEvent> messages = messageStore.get(streamKey);
        if (messages.size() > MESSAGE_RETENTION_SIZE) {
            messages.subList(0, messages.size() - MESSAGE_RETENTION_SIZE).clear();  // 오래된 메시지 삭제
        }
    }

    private void replayMessages(String clientId, String lastEventId, SseEmitter emitter) {
        // 클라이언트별 스트림과 브로드캐스트 스트림 모두 확인
        List<String> streamKeys = Arrays.asList(
                STREAM_PREFIX + clientId,
                STREAM_PREFIX + "broadcast"
        );

        for (String streamKey : streamKeys) {
            List<SseEvent> messages = messageStore.getOrDefault(streamKey, new ArrayList<>());
            boolean startReplay = false;

            for (SseEvent event : messages) {
                if (startReplay) {
                    sendEventToEmitter(emitter, clientId, event);
                }
                if (event.getId().equals(lastEventId)) {
                    startReplay = true;
                }
            }
        }
    }

    private void sendEventToEmitter(SseEmitter emitter, String clientId, SseEvent event) {
        executorService.execute(() -> {
            try {
                eventProcessor.handleEvent(emitter, clientId, event);
            } catch (Exception e) {
                logger.error("Failed to send event to client: {}", clientId, e);
                connectionManager.removeClient(clientId);
            }
        });
    }

    private void updateConsumerOffset(String clientId, String eventId) {
        consumerOffsets.put(clientId, eventId);
    }

    private void deliverMessageToClient(String clientId, SseEvent event) {
        SseEmitter emitter = connectionManager.getLocalEmitter(clientId);
        if (emitter != null) {
            sendEventToEmitter(emitter, clientId, event);
            updateConsumerOffset(clientId, event.getId());
        }
    }

    private void deliverBroadcastMessage(SseEvent event) {
        connectionManager.getLocalEmitterALL().forEach((clientId, emitter) -> {
            sendEventToEmitter(emitter, clientId, event);
            updateConsumerOffset(clientId, event.getId());
        });
    }
} 