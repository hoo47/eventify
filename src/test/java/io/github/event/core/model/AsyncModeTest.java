package io.github.event.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AsyncModeTest {

    @Test
    void testEnumValues() {
        AsyncMode[] modes = AsyncMode.values();
        
        assertThat(modes).hasSize(2);
        assertThat(modes).contains(
            AsyncMode.EXECUTOR,
            AsyncMode.RABBITMQ
        );
    }
    
    @Test
    void testEnumValueOf() {
        assertThat(AsyncMode.valueOf("EXECUTOR")).isEqualTo(AsyncMode.EXECUTOR);
        assertThat(AsyncMode.valueOf("RABBITMQ")).isEqualTo(AsyncMode.RABBITMQ);
    }
    
    @Test
    void testEnumOrdinals() {
        assertThat(AsyncMode.EXECUTOR.ordinal()).isEqualTo(0);
        assertThat(AsyncMode.RABBITMQ.ordinal()).isEqualTo(1);
    }
    
    @Test
    void testEnumToString() {
        assertThat(AsyncMode.EXECUTOR.toString()).isEqualTo("EXECUTOR");
        assertThat(AsyncMode.RABBITMQ.toString()).isEqualTo("RABBITMQ");
    }
    
    @Test
    void testEnumEquality() {
        AsyncMode executor1 = AsyncMode.EXECUTOR;
        AsyncMode executor2 = AsyncMode.valueOf("EXECUTOR");
        AsyncMode rabbitmq = AsyncMode.RABBITMQ;
        
        assertThat(executor1).isEqualTo(executor2);
        assertThat(executor1).isNotEqualTo(rabbitmq);
        assertThat(executor1 == executor2).isTrue();
    }
} 