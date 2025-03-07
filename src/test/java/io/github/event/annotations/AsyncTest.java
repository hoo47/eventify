package io.github.event.annotations;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class AsyncTest {

    static class DummyAsyncListener {
        @Async
        public void onAsyncEvent(Object event) {
            // 비동기 이벤트 처리 메서드
        }
        
        public void noAsyncEvent(Object event) {
            // @Async 어노테이션이 없는 메서드
        }
    }

    @Test
    public void testAsyncAnnotationPresent() throws Exception {
        Method asyncMethod = DummyAsyncListener.class.getMethod("onAsyncEvent", Object.class);
        assertThat(asyncMethod.isAnnotationPresent(Async.class))
            .as("onAsyncEvent 메서드에 @Async 어노테이션이 존재해야 합니다.")
            .isTrue();
        
        Method nonAsyncMethod = DummyAsyncListener.class.getMethod("noAsyncEvent", Object.class);
        assertThat(nonAsyncMethod.isAnnotationPresent(Async.class))
            .as("noAsyncEvent 메서드에는 @Async 어노테이션이 없어야 합니다.")
            .isFalse();
    }
} 