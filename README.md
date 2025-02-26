# Eventify

Eventify는 Java 애플리케이션을 위한 강력하고 유연한 이벤트 처리 라이브러리입니다. 동기 및 비동기 이벤트 처리, 트랜잭션 지원, 멱등성 보장, RabbitMQ 통합 등 다양한 기능을 제공합니다.

## 주요 기능

- **다양한 이벤트 처리 모드**: 동기, 비동기, 트랜잭션 기반 이벤트 처리 지원
- **어노테이션 기반 이벤트 핸들러**: 간단한 어노테이션으로 이벤트 핸들러 등록
- **멱등성 보장**: 이벤트 중복 처리 방지 기능
- **RabbitMQ 통합**: 메시지 브로커를 통한 분산 이벤트 처리 지원
- **트랜잭션 지원**: 트랜잭션 단계에 따른 이벤트 발행 및 처리

## 시작하기

### 의존성 추가

Gradle을 사용하는 경우:

```gradle
implementation 'io.github.eventify:eventify:1.0.0'
```

Maven을 사용하는 경우:

```xml
<dependency>
    <groupId>io.github</groupId>
    <artifactId>eventify</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 기본 설정

Eventify 인스턴스를 생성하고 설정합니다:

```java
// 트랜잭션 관리자 생성
TransactionManager transactionManager = new JdbcTransactionManager(dataSource);

// Eventify 인스턴스 생성
Eventify eventify = Eventify.builder()
    .transactionManager(transactionManager)
    .build();

// 이벤트 발행자와 핸들러 레지스트리 가져오기
EventPublisher publisher = eventify.getEventPublisher();
EventHandlerRegistry registry = eventify.getHandlerRegistry();
```

### RabbitMQ 통합 설정

RabbitMQ를 사용하여 분산 이벤트 처리를 설정하려면:

```java
Eventify eventify = Eventify.builder()
    .transactionManager(transactionManager)
    .enableRabbitMQ(true)
    .rabbitMQHost("localhost")
    .rabbitMQPort(5672)
    .rabbitMQUsername("guest")
    .rabbitMQPassword("guest")
    .rabbitMQExchange("events")
    .rabbitMQQueue("events.queue")
    .asyncMode(AsyncMode.RABBITMQ)
    .build();
```

## 이벤트 정의

이벤트 클래스를 정의합니다:

```java
public class UserCreatedEvent extends AbstractEvent {
    private final String userId;
    private final String username;
    
    public UserCreatedEvent(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}
```

## 이벤트 핸들러 정의

어노테이션을 사용하여 이벤트 핸들러를 정의합니다:

```java
@EventHandler
public class UserEventHandler {
    
    @ListenerMethod
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("사용자 생성됨: " + event.getUsername());
    }
    
    @AsyncListener
    public void sendWelcomeEmail(UserCreatedEvent event) {
        System.out.println("비동기로 환영 이메일 전송 중: " + event.getUsername());
        // 이메일 전송 로직
    }
    
    @TransactionalListener
    public void updateUserStats(UserCreatedEvent event) {
        System.out.println("트랜잭션 내에서 사용자 통계 업데이트 중: " + event.getUsername());
        // 통계 업데이트 로직
    }
}
```

## 이벤트 핸들러 등록

이벤트 핸들러를 등록합니다:

```java
UserEventHandler handler = new UserEventHandler();
registry.registerEventHandler(handler);
```

## 이벤트 발행

이벤트를 발행합니다:

```java
// 동기 이벤트 발행
UserCreatedEvent event = new UserCreatedEvent("123", "user123");
publisher.publishEvent(event);

// 비동기 이벤트 발행
publisher.publishEventAsync(event);

// 트랜잭션 이벤트 발행
TransactionalEventPublisher txPublisher = eventify.getTransactionalEventPublisher();
txPublisher.publishEvent(event, TransactionPhase.AFTER_COMMIT);
```

## 멱등성 처리

중복 이벤트 처리를 방지하려면:

```java
@EventHandler
public class PaymentEventHandler {
    
    @Idempotent
    @ListenerMethod
    public void processPayment(PaymentEvent event) {
        // 이 메서드는 동일한 이벤트에 대해 한 번만 실행됩니다
        System.out.println("결제 처리 중: " + event.getPaymentId());
    }
}
```

## 고급 설정

커스텀 비동기 실행기를 설정하려면:

```java
Executor customExecutor = Executors.newFixedThreadPool(10);

Eventify eventify = Eventify.builder()
    .transactionManager(transactionManager)
    .asyncExecutor(customExecutor)
    .build();
```

커스텀 이벤트 저장소를 설정하려면:

```java
EventRepository customRepository = new CustomEventRepository();

Eventify eventify = Eventify.builder()
    .transactionManager(transactionManager)
    .eventRepository(customRepository)
    .build();
```

## 종료

애플리케이션 종료 시 Eventify 인스턴스를 정리합니다:

```java
eventify.shutdown();
```

## 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 LICENSE 파일을 참조하세요.