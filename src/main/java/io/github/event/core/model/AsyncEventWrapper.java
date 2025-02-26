package io.github.event.core.model;

import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * 비동기 이벤트 래퍼 클래스
 * 이벤트와 리스너를 함께 직렬화하기 위한 래퍼
 */
public class AsyncEventWrapper<T extends Event> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private final T event;
    private final String listenerClassName;
    private final String listenerMethodName;
    private final TransactionPhase transactionPhase;
    
    @JsonIgnore
    private transient EventListener<T> listener;
    
    /**
     * 이벤트와 리스너를 래핑하는 생성자
     * 
     * @param event 이벤트
     * @param listener 리스너
     */
    public AsyncEventWrapper(T event, EventListener<T> listener) {
        this(event, listener, null);
    }
    
    /**
     * 이벤트, 리스너, 트랜잭션 단계를 래핑하는 생성자
     * 
     * @param event 이벤트
     * @param listener 리스너
     * @param phase 트랜잭션 단계
     */
    public AsyncEventWrapper(T event, EventListener<T> listener, TransactionPhase phase) {
        this.event = event;
        this.listener = listener;
        this.listenerClassName = listener != null ? listener.getClass().getName() : null;
        this.listenerMethodName = "onEvent"; // 기본 메서드 이름
        this.transactionPhase = phase;
    }
    
    /**
     * 이벤트, 리스너, 메서드 이름, 트랜잭션 단계를 래핑하는 생성자
     * 
     * @param event 이벤트
     * @param listener 리스너
     * @param methodName 리스너 메서드 이름
     * @param phase 트랜잭션 단계
     */
    public AsyncEventWrapper(T event, EventListener<T> listener, String methodName, TransactionPhase phase) {
        this.event = event;
        this.listener = listener;
        this.listenerClassName = listener != null ? listener.getClass().getName() : null;
        this.listenerMethodName = methodName != null ? methodName : "onEvent";
        this.transactionPhase = phase;
    }
    
    /**
     * JSON 역직렬화를 위한 생성자
     */
    @JsonCreator
    public AsyncEventWrapper(
            @JsonProperty("event") T event,
            @JsonProperty("listenerClassName") String listenerClassName,
            @JsonProperty("listenerMethodName") String listenerMethodName,
            @JsonProperty("transactionPhase") TransactionPhase transactionPhase) {
        this.event = event;
        this.listenerClassName = listenerClassName;
        this.listenerMethodName = listenerMethodName != null ? listenerMethodName : "onEvent";
        this.transactionPhase = transactionPhase;
    }
    
    public T getEvent() {
        return event;
    }
    
    public EventListener<T> getListener() {
        return listener;
    }
    
    public String getListenerClassName() {
        return listenerClassName;
    }
    
    public String getListenerMethodName() {
        return listenerMethodName;
    }
    
    public TransactionPhase getTransactionPhase() {
        return transactionPhase;
    }
    
    public void setListener(EventListener<T> listener) {
        this.listener = listener;
    }
} 