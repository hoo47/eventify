package io.github.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.event.core.api.EventDispatcher;
import io.github.event.core.api.EventPublisher;
import io.github.event.core.api.TransactionalEventPublisher;
import io.github.event.core.model.AsyncEventWrapper;
import io.github.event.core.model.AsyncEventWrapperMixin;
import io.github.event.core.model.AsyncMode;
import io.github.event.dispatcher.CompositeEventDispatcher;
import io.github.event.dispatcher.DefaultEventDispatcher;
import io.github.event.dispatcher.DispatcherSelectionStrategy;
import io.github.event.dispatcher.RabbitMQEventDispatcher;
import io.github.event.idempotent.EventRepository;
import io.github.event.idempotent.IdempotentEventProcessor;
import io.github.event.idempotent.InMemoryEventRepository;
import io.github.event.listener.ApplicationMulticaster;
import io.github.event.listener.EventHandlerRegistry;
import io.github.event.publisher.DefaultEventPublisher;
import io.github.event.publisher.TransactionalEventPublisherImpl;
import io.github.event.rabbitmq.RabbitMQConfig;
import io.github.event.rabbitmq.RabbitMQConnectionManager;
import io.github.event.rabbitmq.RabbitMQEventListener;
import io.github.event.rabbitmq.RabbitMQEventPublisher;
import io.github.event.transaction.TransactionManager;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Eventify {
    
    private final ApplicationMulticaster multicaster;
    private final EventPublisher eventPublisher;
    private final EventHandlerRegistry handlerRegistry;
    private final RabbitMQEventPublisher rabbitMQEventPublisher;
    private final RabbitMQEventListener rabbitMQEventListener;
    private final RabbitMQConnectionManager rabbitMQConnectionManager;
    private final RabbitMQConfig rabbitMQConfig;
    private final EventDispatcher eventDispatcher;
    
    private Eventify(Builder builder) {
        // 기본 컴포넌트 초기화
        Executor asyncExecutor = builder.asyncExecutor != null ? 
                builder.asyncExecutor : 
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        
        IdempotentEventProcessor idempotentProcessor = new IdempotentEventProcessor(
                builder.eventRepository != null ? 
                        builder.eventRepository : 
                        new InMemoryEventRepository()
        );
        
        // RabbitMQ 컴포넌트 초기화 (설정된 경우)
        RabbitMQEventPublisher rabbitMQPublisher = null;
        RabbitMQConnectionManager connectionManager = null;
        RabbitMQConfig rabbitConfig = null;
        
        if (builder.enableRabbitMQ) {
            connectionManager = new RabbitMQConnectionManager(
                    builder.rabbitMQHost,
                    builder.rabbitMQPort,
                    builder.rabbitMQUsername,
                    builder.rabbitMQPassword
            );
            
            rabbitConfig = new RabbitMQConfig(
                    connectionManager,
                    builder.rabbitMQExchange,
                    builder.rabbitMQQueue
            );
            
            try {
                rabbitConfig.initialize();
            } catch (IOException | TimeoutException e) {
                log.error("RabbitMQ 초기화 실패: {}", e.getMessage(), e);
                throw new RuntimeException("RabbitMQ 초기화 실패", e);
            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            
            // Jackson 버전에 따라 다른 설정 사용
            try {
                // Jackson 2.10.0 이상 버전용 설정 시도
                objectMapper.activateDefaultTyping(
                    objectMapper.getPolymorphicTypeValidator(),
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
                );
            } catch (NoSuchMethodError e) {
                // 이전 버전용 설정으로 폴백
                objectMapper.enableDefaultTyping(
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
                );
            }
            
            // 믹스인 등록
            objectMapper.addMixIn(AsyncEventWrapper.class, AsyncEventWrapperMixin.class);
            
            rabbitMQPublisher = new RabbitMQEventPublisher(
                    connectionManager,
                    objectMapper,
                    builder.rabbitMQExchange
            );
        }
        
        this.rabbitMQConnectionManager = connectionManager;
        this.rabbitMQConfig = rabbitConfig;
        this.rabbitMQEventPublisher = rabbitMQPublisher;
        
        // 멀티캐스터 생성 (순환 의존성 해결을 위해 먼저 생성)
        ApplicationMulticaster appMulticaster = new ApplicationMulticaster(null, idempotentProcessor);
        this.multicaster = appMulticaster;
        
        // 트랜잭션 발행자 생성
        TransactionalEventPublisher transactionalPublisher =
                new TransactionalEventPublisherImpl(appMulticaster, builder.transactionManager, asyncExecutor);
        
        // 이벤트 디스패처 생성
        Map<AsyncMode, EventDispatcher> dispatchers = new HashMap<>();
        
        // 기본 디스패처 생성
        DefaultEventDispatcher defaultDispatcher = new DefaultEventDispatcher(
                asyncExecutor, 
                transactionalPublisher
        );
        dispatchers.put(AsyncMode.EXECUTOR, defaultDispatcher);
        
        // RabbitMQ 디스패처 생성 (설정된 경우)
        if (builder.enableRabbitMQ && rabbitMQPublisher != null) {
            RabbitMQEventDispatcher rabbitMQDispatcher = new RabbitMQEventDispatcher(
                    rabbitMQPublisher, 
                    defaultDispatcher
            );
            dispatchers.put(AsyncMode.RABBITMQ, rabbitMQDispatcher);
        } else {
            // RabbitMQ가 설정되지 않은 경우 기본 디스패처를 사용
            dispatchers.put(AsyncMode.RABBITMQ, defaultDispatcher);
        }
        
        // 디스패처 선택 전략 생성
        DispatcherSelectionStrategy selectionStrategy = new DispatcherSelectionStrategy(
                dispatchers, 
                builder.asyncMode
        );
        
        // 컴포지트 디스패처 생성
        EventDispatcher dispatcher = new CompositeEventDispatcher(selectionStrategy);
        this.eventDispatcher = dispatcher;
        
        // 멀티캐스터에 디스패처 설정
        appMulticaster.setEventDispatcher(dispatcher);
        
        // 이벤트 발행자 및 핸들러 레지스트리 생성
        this.eventPublisher = new DefaultEventPublisher(appMulticaster);
        this.handlerRegistry = new EventHandlerRegistry(appMulticaster);
        
        // RabbitMQ 리스너 생성 및 시작 (설정된 경우)
        if (builder.enableRabbitMQ && rabbitMQPublisher != null) {
            RabbitMQEventListener listener = new RabbitMQEventListener(
                    connectionManager,
                    ((RabbitMQEventPublisher) rabbitMQPublisher).getObjectMapper(),
                    this.eventPublisher,
                    builder.rabbitMQQueue,
                    this.handlerRegistry
            );
            
            try {
                listener.start();
            } catch (IOException | TimeoutException e) {
                log.error("RabbitMQ 리스너 시작 실패: {}", e.getMessage(), e);
                throw new RuntimeException("RabbitMQ 리스너 시작 실패", e);
            }
            
            this.rabbitMQEventListener = listener;
        } else {
            this.rabbitMQEventListener = null;
        }
    }
    
    public EventPublisher getEventPublisher() {
        return eventPublisher;
    }
    
    public EventHandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }
    
    public void shutdown() {
        if (rabbitMQEventListener != null) {
            rabbitMQEventListener.stop();
        }
        
        if (rabbitMQConnectionManager != null) {
            rabbitMQConnectionManager.close();
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Setter
    @Accessors(fluent = true)
    public static class Builder {
        private Executor asyncExecutor;
        private TransactionManager transactionManager;
        private EventRepository eventRepository;
        private boolean enableRabbitMQ = false;
        private String rabbitMQHost = "localhost";
        private int rabbitMQPort = 5672;
        private String rabbitMQUsername = "guest";
        private String rabbitMQPassword = "guest";
        private String rabbitMQExchange = "events";
        private String rabbitMQQueue = "events.queue";
        private AsyncMode asyncMode = AsyncMode.EXECUTOR;
        
        public Eventify build() {
            if (transactionManager == null) {
                throw new IllegalStateException("트랜잭션 관리자가 설정되지 않았습니다.");
            }
            
            return new Eventify(this);
        }
    }
} 
