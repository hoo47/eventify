package io.github.event.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class AsyncEventWrapperMixin {
    // 믹스인 클래스는 구현이 필요 없음
} 