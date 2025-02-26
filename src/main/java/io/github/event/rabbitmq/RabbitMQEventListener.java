package io.github.event.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventPublisher;
import io.github.event.core.model.AsyncEventWrapper;
import io.github.event.listener.EventHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.lang.reflect.Method;

public class RabbitMQEventListener {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQEventListener.class);
    
    private final RabbitMQConnectionManager connectionManager;
    private final ObjectMapper objectMapper;
    private final EventPublisher localEventPublisher;
    private final String queueName;
    private final EventHandlerRegistry handlerRegistry;
    private final ExecutorService executorService;
    
    private Channel channel;
    
    public RabbitMQEventListener(
            RabbitMQConnectionManager connectionManager,
            ObjectMapper objectMapper,
            EventPublisher localEventPublisher,
            String queueName,
            EventHandlerRegistry handlerRegistry) {
        this.connectionManager = connectionManager;
        this.objectMapper = objectMapper;
        this.localEventPublisher = localEventPublisher;
        this.queueName = queueName;
        this.handlerRegistry = handlerRegistry;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }
    
    public void start() throws IOException, TimeoutException {
        channel = connectionManager.createChannel();
        
        // 컨슈머 설정
        channel.basicQos(1); // 한 번에 하나의 메시지만 처리
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                Map<String, Object> headers = delivery.getProperties().getHeaders();
                
                String eventType = headers.get("X-Event-Type").toString();
                
                // 비동기 이벤트 래퍼 처리
                if (AsyncEventWrapper.class.getName().equals(eventType)) {
                    processAsyncEventWrapper(message, headers);
                } else {
                    // 일반 이벤트 처리 (기존 코드)
                    Class<?> eventClass = Class.forName(eventType);
                    Event event = (Event) objectMapper.readValue(message, eventClass);
                    log.debug("RabbitMQ에서 이벤트 수신: {}, ID: {}", 
                            event.getClass().getSimpleName(), event.getEventId());
                    
                    // 로컬 이벤트 시스템으로 전달
                    localEventPublisher.publish(event);
                }
                
                // 메시지 확인
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                log.error("RabbitMQ 이벤트 처리 실패: {}", e.getMessage(), e);
                // 메시지 거부 (재큐)
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
            }
        };
        
        CancelCallback cancelCallback = consumerTag -> 
                log.warn("RabbitMQ 컨슈머 취소됨: {}", consumerTag);
        
        channel.basicConsume(queueName, false, deliverCallback, cancelCallback);
        log.info("RabbitMQ 이벤트 리스너 시작됨: {}", queueName);
    }
    
    public void stop() {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
                log.info("RabbitMQ 이벤트 리스너 중지됨");
            } catch (IOException | TimeoutException e) {
                log.error("RabbitMQ 채널 종료 실패: {}", e.getMessage(), e);
            }
        }
        
        executorService.shutdown();
    }
    
    /**
     * 비동기 이벤트 래퍼를 처리합니다.
     */
    private void processAsyncEventWrapper(String message, Map<String, Object> headers) throws Exception {
        // 비동기 이벤트 래퍼 역직렬화
        AsyncEventWrapper<?> wrapper = objectMapper.readValue(message, AsyncEventWrapper.class);
        
        // 리스너 클래스 로드
        Class<?> listenerClass = Class.forName(wrapper.getListenerClassName());
        Object listenerInstance = getListenerInstance(listenerClass);
        
        // 메서드 찾기
        Method method = findMethod(listenerClass, wrapper.getListenerMethodName(), wrapper.getEvent().getClass());
        
        // 이벤트 처리 메서드 호출
        method.setAccessible(true);
        try {
            // 이벤트 처리
            method.invoke(listenerInstance, wrapper.getEvent());
            
            // 성공 로그
            log.debug("RabbitMQ에서 비동기 이벤트 처리: {}, 리스너: {}.{}, ID: {}", 
                    wrapper.getEvent().getClass().getSimpleName(),
                    listenerClass.getSimpleName(),
                    wrapper.getListenerMethodName(),
                    wrapper.getEvent().getEventId());
        } catch (Exception e) {
            // 오류 로그
            log.error("비동기 이벤트 처리 실패: {}, 리스너: {}.{}, ID: {}", 
                    wrapper.getEvent().getClass().getSimpleName(),
                    listenerClass.getSimpleName(),
                    wrapper.getListenerMethodName(),
                    e.getMessage(), e);
            
            // 재시도 로직 또는 데드 레터 큐로 이동
            throw e;
        }
    }
    
    /**
     * 리스너 인스턴스를 가져옵니다. 
     * 애플리케이션 컨텍스트나 팩토리에서 가져오거나 새로 생성합니다.
     */
    private Object getListenerInstance(Class<?> listenerClass) throws Exception {
        // EventHandlerRegistry에서 인스턴스 가져오기
        Object instance = handlerRegistry.getHandler(listenerClass);
        
        // 등록된 인스턴스가 없으면 새로 생성
        if (instance == null) {
            instance = listenerClass.getDeclaredConstructor().newInstance();
            // 새로 생성한 인스턴스를 등록
            handlerRegistry.registerHandler(instance);
        }
        
        return instance;
    }
    
    /**
     * 이벤트 처리 메서드를 찾습니다.
     */
    private Method findMethod(Class<?> listenerClass, String methodName, Class<?> eventType) {
        for (Method method : listenerClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && 
                method.getParameterCount() == 1 && 
                method.getParameterTypes()[0].isAssignableFrom(eventType)) {
                return method;
            }
        }
        throw new IllegalArgumentException("이벤트 처리 메서드를 찾을 수 없습니다: " + 
                listenerClass.getName() + "." + methodName);
    }
} 
