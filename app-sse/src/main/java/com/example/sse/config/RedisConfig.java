package com.example.sse.config;

import com.example.sse.model.RedisMessage;
import com.example.sse.model.SseEvent;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 관련 설정을 정의하는 설정 클래스
 * Redis 연결, 메시지 리스너, 직렬화 등의 설정을 담당
 */
@Configuration
public class RedisConfig {

    /**
     * Redis Pub/Sub 메시지를 수신하기 위한 리스너 컨테이너를 생성
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return Redis 메시지 리스너 컨테이너
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    /**
     * Redis 데이터 접근을 위한 RedisTemplate 빈을 생성
     * 키는 문자열, 값은 JSON 형식으로 직렬화
     *
     * @param connectionFactory Redis 연결 팩토리
     * @return 설정된 RedisTemplate 인스턴스
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // RedisMessage 클래스를 위한 JSON 직렬화 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        Jackson2JsonRedisSerializer<RedisMessage> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, RedisMessage.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        // 기본 직렬화 설정 - Streams 도 ValueSerializer 로 동작
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        // Pub/Sub 메시지를 위한 해시 키/값 직렬화 설정
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }
    @Bean
    public RedisTemplate<String, Object> lastEventTemplate(RedisConnectionFactory connectionFactory) {
        // SseEvent 클래스를 위한 JSON 직렬화 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        Jackson2JsonRedisSerializer<SseEvent> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, SseEvent.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }
} 