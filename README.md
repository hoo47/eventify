# Eventify: A Versatile Event-Driven Library for Java

Eventify is a powerful and flexible event processing library for Java applications. It supports synchronous, asynchronous, and transaction-integrated event processing. With annotation-based handler registration and built-in adapter integration, Eventify enables developers to easily implement event-driven architectures.

## Features

- **Multiple Event Processing Modes**: Synchronous, asynchronous, and transaction-integrated event handling.
- **Annotation-Based Handler Registration**: Easily define event handlers using annotations like `@EventListener`, `@TransactionalEventListener`, and `@Async`.
- **Transaction Integration**: Delay event processing during an active transaction and automatically flush events after the transaction commits.
- **Adapter Integration**: Seamlessly integrate with external transaction management systems via adapters (e.g., SpringTransactionManagerAdapter, JakartaTransactionManagerAdapter).
- **Extensibility**: Easily extend functionality with custom logic or external systems.

## Quick Start

### Adding Dependency

Using Gradle:
```gradle
implementation 'io.github.eventify:eventify:1.0.0'
```

Using Maven:
```xml
<dependency>
    <groupId>io.github.eventify</groupId>
    <artifactId>eventify</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Setup

Create and configure an Eventify instance:

```java
// Create a TransactionManager (e.g., using DummyTransactionManager or an adapter)
TransactionManager transactionManager = new DummyTransactionManager();

// Build Eventify instance
Eventify eventify = Eventify.builder()
    .transactionManager(transactionManager)
    .build();

// Obtain the event publisher and handler registry
EventPublisher publisher = eventify.getEventPublisher();
EventHandlerRegistry registry = eventify.getHandlerRegistry();
```

### Defining Events and Handlers

Define an event:

```java
public class UserCreatedEvent {
    private final String userId;
    private final String username;
    
    public UserCreatedEvent(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}
```

Define an event handler:

```java
public class UserEventHandler {
    
    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("User created: " + event.getUsername());
    }
    
    @Async
    @EventListener
    public void sendWelcomeEmail(UserCreatedEvent event) {
        System.out.println("Sending welcome email to: " + event.getUsername());
    }
    
    @TransactionalEventListener(phase = TransactionalPhase.AFTER_COMMIT)
    public void updateUserStatistics(UserCreatedEvent event) {
        System.out.println("Updating user stats for: " + event.getUsername());
    }
}
```

Register your handler and publish events:

```java
UserEventHandler handler = new UserEventHandler();
registry.register(handler);

UserCreatedEvent event = new UserCreatedEvent("123", "user123");
publisher.publishEvent(event);
```

### Adapter Integration

Eventify can integrate with external transaction management systems via adapters. For detailed setup instructions, please refer to [ADAPTER_SETUP.md](ADAPTER_SETUP.md).

## License

Eventify is released under the MIT License. See [LICENSE](LICENSE) for details.