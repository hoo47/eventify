package io.github.event.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProcessingModeTest {

    @Test
    void testSynchronousMode() {
        ProcessingMode mode = ProcessingMode.SYNCHRONOUS;
        
        assertThat(mode.isAsync()).isFalse();
        assertThat(mode.isTransactional()).isFalse();
    }
    
    @Test
    void testAsynchronousMode() {
        ProcessingMode mode = ProcessingMode.ASYNCHRONOUS;
        
        assertThat(mode.isAsync()).isTrue();
        assertThat(mode.isTransactional()).isFalse();
    }
    
    @Test
    void testTransactionalMode() {
        ProcessingMode mode = ProcessingMode.TRANSACTIONAL;
        
        assertThat(mode.isAsync()).isFalse();
        assertThat(mode.isTransactional()).isTrue();
    }
    
    @Test
    void testAsyncTransactionalMode() {
        ProcessingMode mode = ProcessingMode.ASYNC_TRANSACTIONAL;
        
        assertThat(mode.isAsync()).isTrue();
        assertThat(mode.isTransactional()).isTrue();
    }
    
    @ParameterizedTest
    @EnumSource(value = ProcessingMode.class, names = {"ASYNCHRONOUS", "ASYNC_TRANSACTIONAL"})
    void testAsyncModes(ProcessingMode mode) {
        assertThat(mode.isAsync()).isTrue();
    }
    
    @ParameterizedTest
    @EnumSource(value = ProcessingMode.class, names = {"SYNCHRONOUS", "TRANSACTIONAL"})
    void testSyncModes(ProcessingMode mode) {
        assertThat(mode.isAsync()).isFalse();
    }
    
    @ParameterizedTest
    @EnumSource(value = ProcessingMode.class, names = {"TRANSACTIONAL", "ASYNC_TRANSACTIONAL"})
    void testTransactionalModes(ProcessingMode mode) {
        assertThat(mode.isTransactional()).isTrue();
    }
    
    @ParameterizedTest
    @EnumSource(value = ProcessingMode.class, names = {"SYNCHRONOUS", "ASYNCHRONOUS"})
    void testNonTransactionalModes(ProcessingMode mode) {
        assertThat(mode.isTransactional()).isFalse();
    }
} 