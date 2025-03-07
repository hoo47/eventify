package io.github.event.annotations;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class TransactionalEventListenerTest {

    static class DummyTransactionalListener {
        @TransactionalEventListener(phase = TransactionalPhase.BEFORE_COMMIT)
        public void onTransactionalEvent(Object event) {
            // 트랜잭션 이벤트 처리 메서드
        }
    }

    @Test
    public void testTransactionalEventListenerAnnotationPresent() throws Exception {
        Method method = DummyTransactionalListener.class.getMethod("onTransactionalEvent", Object.class);
        assertThat(method.isAnnotationPresent(TransactionalEventListener.class))
            .as("onTransactionalEvent 메서드에 @TransactionalEventListener 어노테이션이 있어야 합니다.")
            .isTrue();
        
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);
        assertThat(annotation)
            .as("어노테이션은 null이 아니어야 합니다.")
            .isNotNull();
        
        assertThat(annotation.phase())
            .as("phase는 BEFORE_COMMIT 이어야 합니다.")
            .isEqualTo(TransactionalPhase.BEFORE_COMMIT);
    }
} 