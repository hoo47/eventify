# EDA Architecture

## Overview

This library implements an event-driven application (EDA) framework in Java, inspired by Spring's event handling features, but built without a dependency on Spring. It supports both synchronous and asynchronous event processing, as well as transactionally synchronized event handling.

## Components

- **Annotations**
  - `@EventListener`: For synchronous event handling.
  - `@TransactionalEventListener`: For transaction-aware event handling, with a configurable phase.
  - `TransactionalPhase`: Enum describing when an event handler should run (e.g., BEFORE_COMMIT, AFTER_COMMIT, AFTER_ROLLBACK, AFTER_COMPLETION).
  - `@Async`: Marks event handling methods to be executed asynchronously.

- **Handler Registry**
  - `EventRegistry`: Maintains registered event handler methods discovered from listeners.
  - `HandlerMethod`: Encapsulates a listener instance and a method.

- **Event Publisher**
  - `DefaultEventPublisher`: Retrieves handler methods from the registry and invokes them when an event is published.

- **Transaction Management**
  - `TransactionManager`: Interface for managing transactions (begin, commit, rollback).
  - `DummyTransactionManager`: A sample implementation to simulate transaction behavior.

- **Async Execution**
  - `AsyncExecutor`: Provides a cached thread pool for executing tasks asynchronously.

## Design Patterns

- **Observer Pattern:** Listeners (observers) are automatically notified when events are published.
- **Strategy Pattern:** Different strategies can be applied for handling various transaction phases and asynchronous execution.
- **Factory Pattern:** Components like the Executor and TransactionManager can be customized and injected as needed.

## Testing

All core functionalities are verified using TDD style tests with AssertJ assertions. Tests cover annotations, handler registration, event publishing, transaction management, and asynchronous execution.

## Usage

Developers can register event handlers (methods annotated with `@EventListener` or `@TransactionalEventListener`) within their listeners. Use the `DefaultEventPublisher` to dispatch events. The transaction manager and async executor ensure that events are processed at the appropriate phase and execution context. 