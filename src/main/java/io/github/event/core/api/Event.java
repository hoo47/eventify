package io.github.event.core.api;

import java.time.Instant;
import java.util.Map;

import com.google.protobuf.Message;

/**
 * 모든 이벤트의 기본 인터페이스
 */
public interface Event {

    /**
     * 이벤트의 고유 ID를 반환합니다.
     * 
     * @return 이벤트 ID
     */
    String getEventId();

    /**
     * 이벤트가 발행된 시간을 반환합니다.
     * 
     * @return 이벤트 발행 시간
     */
    Instant getIssuedAt();
    
    /**
     * 이벤트 메타데이터를 반환합니다.
     * 
     * @return 이벤트 메타데이터
     */
    Map<String, Object> getMetadata();
    
    /**
     * 이벤트 메타데이터를 설정합니다.
     * 
     * @param key 메타데이터 키
     * @param value 메타데이터 값
     */
    void addMetadata(String key, Object value);
    
    /**
     * 이벤트를 프로토콜 버퍼 메시지로 변환합니다.
     * 기본적으로는 지원하지 않으며, 필요한 경우 구현 클래스에서 오버라이드해야 합니다.
     * 
     * @return 프로토콜 버퍼 메시지
     * @throws UnsupportedOperationException 지원하지 않는 경우
     */
    default Message toProtoMessage() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
