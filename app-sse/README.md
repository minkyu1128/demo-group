# SSE(Server-Sent Events) + Redis 기반 실시간 메시징 시스템

Server-Sent Events 실시간 알림 서비스를 위한 SSE(Server-Sent Events) 구현 프로젝트입니다.   
SSE 만을 사용한 예시와 분산 환경에서 서비스를 제공하기 위한 SSE + Redis 구현 예시를 제공합니다.

## 1. 단일 서버 운영 시 문제점

### 1.1 메모리 기반 연결 관리의 한계

- 서버의 메모리에 SseEmitter 객체를 보관
- 서버 재시작/장애 시 모든 연결이 끊어짐
- 메모리 부족 발생 가능성

### 1.2 이중화 서버 운영 시 발생하는 문제

- 서버 A에 연결된 클라이언트와 서버 B에 연결된 클라이언트 간 통신 불가
- 로드밸런서를 통한 요청 분산 시 세션 관리 어려움
- 전체 공지 등 브로드캐스트 메시지 전파 불가능

## 2. Redis 도입 배경

### 2.1 Redis 선택 이유

- Pub/Sub 패턴을 통한 실시간 메시지 브로드캐스팅 지원
- Redis Streams를 통한 메시지 영속성 및 순서 보장
- 분산 환경에서의 세션 관리 용이
- 높은 성능과 안정성

### 2.2 Redis 주요 활용 기능

1. **Pub/Sub**
    - 실시간 메시지 전파
    - 서버 간 효율적인 통신

2. **Redis Streams**
    - 메시지 영속성 보장
    - Consumer Group을 통한 안정적인 메시지 처리
    - 장애 상황에서의 메시지 재처리

3. **Redis Set**
    - 연결된 클라이언트 목록 관리
    - 중복 연결 방지

## 3. SSE + Redis 워크플로우

### 3.1 단일 서버 환경 (SSE만 사용)

1. **클라이언트 연결 프로세스**
   ```
   1. 클라이언트 SSE 연결 요청 (/api/v1/sse/connect)
   2. 서버에서 SseEmitter 생성
   3. 로컬 메모리에 클라이언트 ID와 SseEmitter 저장
   4. 연결 성공 이벤트 발행
   ```

2. **일대일 메시지 전송**
   ```
   1. 클라이언트 A가 특정 클라이언트에게 메시지 전송 요청
   2. 로컬 메모리에서 대상 클라이언트의 SseEmitter 조회
   3. SseEmitter를 통해 메시지 전송
      - I/O Error 발생 시 로컬 메모리에서 해당 클라이언트 제거
   ```

3. **브로드캐스트 메시지 전송**
   ```
   1. 클라이언트가 전체 메시지 전송 요청
   2. 서버 메모리에 저장된 모든 SseEmitter 조회
   3. 각 SseEmitter를 통해 메시지 전송
      - I/O Error 발생 시 로컬 메모리에서 해당 클라이언트 제거
   ```

4. **장애 처리**
    - 서버 장애 시 모든 연결 끊어짐
    - 클라이언트 재연결 필요
    - 장애 발생 시점의 메시지 유실

### 3.2 이중화 서버 환경 (SSE + Redis)

1. **클라이언트 연결 프로세스**
   ```
   1. 클라이언트 SSE 연결 요청 (/api/v1/sse-redis/connect)
   2. 서버에서 SseEmitter 생성
   3. 로컬 메모리에 SseEmitter 저장
   4. Redis Set에 클라이언트 ID 저장 (전체 클라이언트 목록 관리)
   5. 연결 성공 이벤트 발행
   ```

2. **일대일 메시지 전송**
   ```
   1. 클라이언트 A가 특정 클라이언트에게 메시지 전송 요청 (Server 1)
   2. Redis Pub/Sub 채널에 메시지 발행
   3. 모든 서버가 메시지 수신
   4. 대상 클라이언트가 연결된 서버(Server 2)에서 메시지 전송
      - I/O Error 발생 시 로컬 메모리와 Redis Set에서 해당 클라이언트 제거
   ```

3. **브로드캐스트 메시지 전송**
   ```
   1. 클라이언트가 전체 메시지 전송 요청 (Server 1)
   2. Redis Pub/Sub 채널에 브로드캐스트 메시지 발행
   3. 모든 서버가 메시지 수신
   4. 각 서버에서 연결된 클라이언트들에게 메시지 전송
      - I/O Error 발생 시 로컬 메모리와 Redis Set에서 해당 클라이언트 제거
   ```

4. **장애 처리**
    - 서버 장애 시 다른 서버로 재연결 가능
    - Redis Set을 통한 전체 클라이언트 상태 관리
    - Redis Pub/Sub으로 메시지 전달 보장
    - 클라이언트 자동 재연결 (Last-Event-ID 기반)

### 3.3 주요 차이점

| **구분**        | **단일 서버 (SSE)** | **이중화 서버 (SSE + Redis)** |
|---------------|-----------------|--------------------------|
| **클라이언트 관리**  | 서버 메모리          | Redis Set + 서버 메모리       |
| **메시지 전달**    | 직접 전송           | Redis Pub/Sub 통한 전파      |
| **서버 간 통신**   | 불가능             | Redis 통한 실시간 통신          |
| **장애 복구**     | 수동 재연결 필요       | 자동 재연결 지원                |
| **확장성**       | 단일 서버로 제한       | 수평적 확장 가능                |
| **메시지 전달 보장** | 서버 장애 시 유실      | Redis를 통한 전달 보장          |

## 4. 주요 구현 코드

### 4.1 Redis 설정

   ``` java
   @Configuration
   public class RedisConfig {
      @Bean
      public RedisMessageListenerContainer redisMessageListener() {
      // Redis Pub/Sub 리스너 설정
      }
      @Bean
      public RedisTemplate<String, Object> redisTemplate() {
      // Redis 템플릿 설정
      }
   }
   ```

### 4.2 메시지 처리

   ``` java
   @Service
   public class RedisSseEmitterServiceImpl implements SseEmitterService {
      // Redis Streams 메시지 처리
      @Scheduled(fixedRate = 100)
      public void consumeMessages() {
      // 스트림에서 메시지 읽기 및 처리
      }
      // 실패한 메시지 재처리
      @Scheduled(fixedRate = 60000)
      public void processPendingMessages() {
      // 미처리 메시지 재시도
      }
   }
   ```

## 5. API 엔드포인트

### 5.1 SSE API 목록 - 단일 서버 환경

| 메소드  | 경로                                | 설명                   |
|------|-----------------------------------|----------------------|
| GET  | /api/v1/sse/connect               | SSE 연결 수립            |
| POST | /api/v1/sse/send/{clientId}       | 특정 클라이언트에게 메시지 전송    |
| POST | /api/v1/sse/broadcast             | 전체 클라이언트에게 메시지 전송    |
| GET  | /api/v1/sse/connected-count       | 연결된 클라이언트 수 조회       |
| POST | /api/v1/sse/disconnect/{clientId} | 특정 클라이언트와의 SSE 연결 해제 |
| POST | /api/v1/sse/shutdown              | 전체 클라이언트와의 SSE 연결 해제 |

### 5.2 SSE + Redis API 목록 - 이중화 서버 환경

| 메소드  | 경로                                      | 설명                   |
|------|-----------------------------------------|----------------------|
| GET  | /api/v1/sse-redis/connect               | SSE 연결 수립            |
| POST | /api/v1/sse-redis/send/{clientId}       | 특정 클라이언트에게 메시지 전송    |
| POST | /api/v1/sse-redis/broadcast             | 전체 클라이언트에게 메시지 전송    |
| GET  | /api/v1/sse-redis/connected-count       | 연결된 클라이언트 수 조회       |
| POST | /api/v1/sse-redis/disconnect/{clientId} | 특정 클라이언트와의 SSE 연결 해제 |
| POST | /api/v1/sse-redis/shutdown              | 전체 클라이언트와의 SSE 연결 해제 |

## 6. 실행 방법

### 6.1 애플리케이션 실행

``` bash
./gradlew bootRun
```

## 7. 환경 설정

### application.yml

``` yaml
spring:
   data:
      redis:
         host: localhost
         port: 36379
```

## 8. 참고 자료

### 8.1 Message 브로커 특징 비교

| 특징      | Redis                                  | RabbitMQ      | Kafka           |
|---------|----------------------------------------|---------------|-----------------|
| 아키텍처    | 인메모리 데이터 저장소                           | 큐 기반 메시지 브로커  | 분산 이벤트 스트리밍 플랫폼 |
| 지원 프로토콜 | Pub/Sub, Streams(Redis 5.0부터)          | AMQP          | Pub/Sub         |
| 메시지 큐   | Pub/Sub: X, Streams: O (시간 기반 큐)       | O             | O               |
| 메시지 순서  | Pub/Sub: 보장하지 않음, Streams: 시간 기반 순서    | 큐 내에서 FIFO    | 파티션 내 순서 보장     |
| 처리량     | Pub/Sub: 초당 100만+@, Streams: 초당 수만     | 초당 38MB       | 초당 605MB        |
| 지연시간    | Pub/Sub: 0.2~0.8ms, Streams: 1~2ms     | ~1ms (낮은 부하)  | ~5ms            |
| 메시지 영속성 | Pub/Sub: 없음, Streams: AOF/RDB 지원       | 디스크 저장 지원     | 강력한 영속성 보장      |
| 확장성     | Pub/Sub: 클러스터링, Streams: 클러스터 샤딩       | 수평적 확장 가능     | 매우 높은 확장성       |
| 사용 사례   | Pub/Sub: 실시간 알림/채팅, Streams: 이벤트 소싱/로깅 | 작업 큐, 실시간 메시징 | 로그 수집, 스트림 처리   |
| 복잡도     | Pub/Sub: 매우 단순, Streams: 중간            | 중간            | 높음              |
| 내구성     | Pub/Sub: 보장하지 않음, Streams: AOF/RDB로 보장 | 중간            | 높음              |

### 8.2 Redis Pub/Sub vs Streams 비교

| 특징      | Redis Pub/Sub  | Redis Streams       |
|---------|----------------|---------------------|
| 메시지 저장  | X (휘발성)        | O (영속성)             |
| 메시지 순서  | 보장하지 않음        | 시간 기반 순서 보장         |
| 소비자 그룹  | X              | O (Consumer Groups) |
| 메시지 재처리 | X              | O (재처리 메커니즘 제공)     |
| 실시간성    | 매우 높음          | 높음                  |
| 구현 복잡도  | 낮음             | 중간                  |
| 적합한 사용처 | 실시간 이벤트 브로드캐스팅 | 이벤트 로깅, 메시지 이력 관리   |

**Redis Pub/Sub 특징**

- 메시지가 메모리에만 존재하며 구독자에게 즉시 전달
- 구독자가 없으면 메시지 유실
- 매우 간단한 구현과 높은 성능
- 실시간 알림, 채팅 등에 적합

**Redis Streams 특징**

- 메시지를 저장하고 고유 ID 부여
- Consumer Groups를 통한 메시지 소비 보장
- 장애 복구와 재처리 지원
- 이벤트 소싱, 로깅 등에 적합

**현재 프로젝트의 선택: Redis Pub/Sub**

- SSE의 실시간 특성에 더 적합
- 구현이 단순하고 유지보수가 용이
- 메시지 영속성보다 실시간 전달이 중요한 요구사항
- 클라이언트의 자동 재연결로 일시적인 연결 끊김 처리 가능

**최적 사용 시나리오**

* `Redis Pub/Sub`: 실시간성이 중요한 간단한 메시징
* `Redis Streams`: 메시지 영속성과 처리 보장이 필요한 경우
* `RabbitMQ`: 복잡한 라우팅이 필요한 메시징 시스템
* `Kafka`: 대용량 데이터 처리, 장기 데이터 보관이 필요한 경우

