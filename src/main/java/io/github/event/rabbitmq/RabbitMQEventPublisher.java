package io.github.event.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.api.EventPublisher;
import io.github.event.core.model.AsyncEventWrapper;
import io.github.event.core.model.EventHeaders;
import io.github.event.core.model.TransactionPhase;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RabbitMQEventPublisher implements EventPublisher {

    private final RabbitMQConnectionManager connectionManager;
    @Getter
    private final ObjectMapper objectMapper;
    private final String exchangeName;

    @Override
    public void publish(Event event) {
        publishInternal(event, event.getClass().getSimpleName(), createDefaultHeaders(event));
    }

    @Override
    public <T extends Event> void publish(Event event, EventListener<T> listener) {
        Map<String, Object> headers = createDefaultHeaders(event);
        headers.put("X-Listener-Type", listener.getClass().getName());
        
        String routingKey = listener.getClass().getSimpleName() + "." + event.getClass().getSimpleName();
        publishInternal(event, routingKey, headers);
    }

    public <T extends Event> void publishAsyncEvent(AsyncEventWrapper<T> wrapper) {
        Map<String, Object> headers = createDefaultHeaders(wrapper.getEvent());
        headers.put(EventHeaders.EVENT_TYPE, AsyncEventWrapper.class.getName());
        headers.put("X-Async-Event-Type", wrapper.getEvent().getClass().getName());
        
        String routingKey = "async." + wrapper.getEvent().getClass().getSimpleName();
        publishInternal(wrapper, routingKey, headers);
    }

    public <T extends Event> void publishAsyncEvent(AsyncEventWrapper<T> wrapper, TransactionPhase phase) {
        Map<String, Object> headers = createDefaultHeaders(wrapper.getEvent());
        headers.put(EventHeaders.EVENT_TYPE, AsyncEventWrapper.class.getName());
        headers.put("X-Async-Event-Type", wrapper.getEvent().getClass().getName());
        headers.put("X-Transaction-Phase", phase.name());
        
        String routingKey = String.format("async.%s.%s", 
                phase.name().toLowerCase(),
                wrapper.getEvent().getClass().getSimpleName());
                
        publishInternal(wrapper, routingKey, headers);
    }

    private Map<String, Object> createDefaultHeaders(Event event) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(EventHeaders.EVENT_ID, event.getEventId());
        headers.put(EventHeaders.EVENT_TIMESTAMP, event.getIssuedAt().toString());
        headers.put(EventHeaders.EVENT_TYPE, event.getClass().getName());
        return headers;
    }

    private void publishInternal(Object payload, String routingKey, Map<String, Object> headers) {
        try (Channel channel = connectionManager.createChannel()) {
            String messageJson = objectMapper.writeValueAsString(payload);
            
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .headers(headers)
                    .build();
            
            channel.basicPublish(
                    exchangeName,
                    routingKey,
                    props,
                    messageJson.getBytes(StandardCharsets.UTF_8)
            );
            
            if (payload instanceof AsyncEventWrapper) {
                AsyncEventWrapper<?> wrapper = (AsyncEventWrapper<?>) payload;
                Event event = wrapper.getEvent();
                if (headers.containsKey("X-Transaction-Phase")) {
                    log.debug("RabbitMQ로 트랜잭션 비동기 이벤트 발행: {}, 단계: {}, ID: {}", 
                            event.getClass().getSimpleName(),
                            headers.get("X-Transaction-Phase"),
                            event.getEventId());
                } else {
                    log.debug("RabbitMQ로 비동기 이벤트 발행: {}, ID: {}", 
                            event.getClass().getSimpleName(),
                            event.getEventId());
                }
            } else {
                Event event = (Event) payload;
                if (headers.containsKey("X-Listener-Type")) {
                    log.debug("RabbitMQ로 이벤트 발행: {}, 리스너: {}, ID: {}", 
                            event.getClass().getSimpleName(),
                            headers.get("X-Listener-Type"),
                            event.getEventId());
                } else {
                    log.debug("RabbitMQ로 이벤트 발행: {}, ID: {}", 
                            event.getClass().getSimpleName(),
                            event.getEventId());
                }
            }
        } catch (IOException | TimeoutException e) {
            log.error("RabbitMQ 이벤트 발행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("이벤트 발행 실패", e);
        }
    }
} 
