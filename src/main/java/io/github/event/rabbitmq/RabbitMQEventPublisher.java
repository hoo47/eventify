package io.github.event.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventPublisher;
import io.github.event.core.model.AsyncEventWrapper;
import io.github.event.core.model.EventHeaders;
import io.github.event.core.model.TransactionPhase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Slf4j
@RequiredArgsConstructor
public class RabbitMQEventPublisher implements EventPublisher {
    
    private final RabbitMQConnectionManager connectionManager;
    @Getter
    private final ObjectMapper objectMapper;
    private final String exchangeName;
    
    @Override
    public void publish(Event event) {
        try (Channel channel = connectionManager.createChannel()) {
            String routingKey = event.getClass().getSimpleName();
            String eventJson = objectMapper.writeValueAsString(event);
            
            // 헤더 설정
            Map<String, Object> headers = new HashMap<>();
            headers.put(EventHeaders.EVENT_ID, event.getEventId());
            headers.put(EventHeaders.EVENT_TIMESTAMP, event.getIssuedAt().toString());
            headers.put(EventHeaders.EVENT_TYPE, event.getClass().getName());
            
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();
            
            channel.basicPublish(
                    exchangeName,
                    routingKey,
                    props,
                    eventJson.getBytes(StandardCharsets.UTF_8)
            );
            
            log.debug("RabbitMQ로 이벤트 발행: {}, ID: {}", 
                    event.getClass().getSimpleName(), event.getEventId());
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ 이벤트 발행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }

    /**
     * 비동기 이벤트를 RabbitMQ로 전송합니다.
     * 
     * @param wrapper 비동기 이벤트 래퍼
     */
    public <T extends Event> void publishAsyncEvent(AsyncEventWrapper<T> wrapper) {
        try (Channel channel = connectionManager.createChannel()) {
            String routingKey = "async." + wrapper.getEvent().getClass().getSimpleName();
            String eventJson = objectMapper.writeValueAsString(wrapper);
            
            // 헤더 설정
            Map<String, Object> headers = new HashMap<>();
            headers.put(EventHeaders.EVENT_ID, wrapper.getEvent().getEventId());
            headers.put(EventHeaders.EVENT_TIMESTAMP, wrapper.getEvent().getIssuedAt().toString());
            headers.put(EventHeaders.EVENT_TYPE, AsyncEventWrapper.class.getName());
            headers.put("X-Async-Event-Type", wrapper.getEvent().getClass().getName());
            
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();
            
            channel.basicPublish(
                    exchangeName,
                    routingKey,
                    props,
                    eventJson.getBytes(StandardCharsets.UTF_8)
            );
            
            log.debug("RabbitMQ로 비동기 이벤트 발행: {}, ID: {}", 
                    wrapper.getEvent().getClass().getSimpleName(), 
                    wrapper.getEvent().getEventId());
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ 비동기 이벤트 발행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("비동기 이벤트 발행 실패", e);
        }
    }

    /**
     * 트랜잭션 단계에 따라 비동기 이벤트를 RabbitMQ로 전송합니다.
     * 
     * @param wrapper 비동기 이벤트 래퍼
     * @param phase 트랜잭션 단계
     */
    public <T extends Event> void publishAsyncEvent(AsyncEventWrapper<T> wrapper, TransactionPhase phase) {
        // 트랜잭션 단계 정보 추가
        try (Channel channel = connectionManager.createChannel()) {
            String routingKey = "async." + phase.name().toLowerCase() + "." + 
                    wrapper.getEvent().getClass().getSimpleName();
            String eventJson = objectMapper.writeValueAsString(wrapper);
            
            // 헤더 설정
            Map<String, Object> headers = new HashMap<>();
            headers.put(EventHeaders.EVENT_ID, wrapper.getEvent().getEventId());
            headers.put(EventHeaders.EVENT_TIMESTAMP, wrapper.getEvent().getIssuedAt().toString());
            headers.put(EventHeaders.EVENT_TYPE, AsyncEventWrapper.class.getName());
            headers.put("X-Async-Event-Type", wrapper.getEvent().getClass().getName());
            headers.put("X-Transaction-Phase", phase.name());
            
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();
            
            channel.basicPublish(
                    exchangeName,
                    routingKey,
                    props,
                    eventJson.getBytes(StandardCharsets.UTF_8)
            );
            
            log.debug("RabbitMQ로 트랜잭션 비동기 이벤트 발행: {}, 단계: {}, ID: {}", 
                    wrapper.getEvent().getClass().getSimpleName(),
                    phase,
                    wrapper.getEvent().getEventId());
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ 트랜잭션 비동기 이벤트 발행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("트랜잭션 비동기 이벤트 발행 실패", e);
        }
    }
} 