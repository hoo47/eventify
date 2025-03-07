package io.github.event.annotations;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class EventListenerTest {

    static class DummyListener {
        @EventListener
        public void onEvent(Object event) {
            // 이벤트 처리 메서드
        }
        
        public void notAnEventListener(Object event) {
            // 어노테이션이 없는 메서드
        }
    }

    @Test
    public void testEventListenerAnnotationPresent() throws Exception {
        Method onEventMethod = DummyListener.class.getMethod("onEvent", Object.class);
        assertThat(onEventMethod.isAnnotationPresent(EventListener.class))
            .as("onEvent 메서드에 @EventListener 어노테이션이 있어야 합니다.")
            .isTrue();

        Method notAnEventMethod = DummyListener.class.getMethod("notAnEventListener", Object.class);
        assertThat(notAnEventMethod.isAnnotationPresent(EventListener.class))
            .as("notAnEventListener 메서드에는 @EventListener 어노테이션이 없어야 합니다.")
            .isFalse();
    }
} 
