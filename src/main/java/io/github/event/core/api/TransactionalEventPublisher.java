package io.github.event.core.api;

import io.github.event.core.model.TransactionPhase;
import java.util.concurrent.CompletableFuture;

/**
 * 트랜잭션 단계에 따라 이벤트를 발행하는 인터페이스
 */
public interface TransactionalEventPublisher {
    
    /**
     * 트랜잭션 단계에 따라 이벤트를 발행합니다.
     * 
     * @param event 발행할 이벤트
     * @param phase 트랜잭션 단계
     */
    void publishInTransaction(Event event, TransactionPhase phase);
    
    /**
     * 트랜잭션 단계에 따라 이벤트를 비동기적으로 발행합니다.
     * 
     * @param event 발행할 이벤트
     * @param phase 트랜잭션 단계
     * @return 비동기 작업의 Future
     */
    CompletableFuture<Void> publishAsyncInTransaction(Event event, TransactionPhase phase);
    
    /**
     * 트랜잭션 단계에 따라 이벤트를 특정 리스너에게 발행합니다.
     * 
     * @param event 발행할 이벤트
     * @param phase 트랜잭션 단계
     * @param listener 이벤트를 처리할 리스너
     */
    <T extends Event> void publishInTransaction(Event event, TransactionPhase phase, EventListener<T> listener);
    
    /**
     * 트랜잭션 단계에 따라 이벤트를 특정 리스너에게 비동기적으로 발행합니다.
     * 
     * @param event 발행할 이벤트
     * @param phase 트랜잭션 단계
     * @param listener 이벤트를 처리할 리스너
     */
    <T extends Event> void publishAsyncInTransaction(Event event, TransactionPhase phase, EventListener<T> listener);
} 