# Eventify Adapter 작성 가이드

Eventify는 트랜잭션 연동 기능을 위해 자체 `TransactionManager` 인터페이스를 사용합니다.
이 인터페이스는 다음의 메소드를 포함합니다:
- `begin()`
- `commit()`
- `rollback()`

기본적으로 개발 또는 테스트 환경에서는 `DummyTransactionManager`와 같은 간단한 구현체를 사용할 수 있지만,
실제 환경(예: Jakarta Persistence/JTA 또는 Spring Boot 환경)에서는 컨테이너나 외부 트랜잭션 관리자의 기능을 활용해야 합니다.

이 가이드는 두 가지 환경에 대한 어댑터 작성 방법을 설명합니다.

---

## 1. Spring Boot/JPA 환경용 어댑터

스프링 부트 환경에서는 스프링이 제공하는 `PlatformTransactionManager`를 활용할 수 있습니다.
이미 제공된 `SpringTransactionManagerAdapter`는 스프링의 트랜잭션 매니저를 eventify의 `TransactionManager` 인터페이스에 맞게 어댑팅합니다.

### 예제: SpringTransactionManagerAdapter

```java
package io.github.event.transaction;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public class SpringTransactionManagerAdapter implements TransactionManager {
    private final PlatformTransactionManager platformTransactionManager;
    private final ThreadLocal<TransactionStatus> currentTransaction = new ThreadLocal<>();

    public SpringTransactionManagerAdapter(PlatformTransactionManager platformTransactionManager) {
        this.platformTransactionManager = platformTransactionManager;
    }

    @Override
    public void begin() {
        TransactionStatus status = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
        currentTransaction.set(status);
    }

    @Override
    public void commit() {
        TransactionStatus status = currentTransaction.get();
        if (status != null) {
            platformTransactionManager.commit(status);
            currentTransaction.remove();
        } else {
            throw new IllegalStateException("No transaction is active");
        }
    }

    @Override
    public void rollback() {
        TransactionStatus status = currentTransaction.get();
        if (status != null) {
            platformTransactionManager.rollback(status);
            currentTransaction.remove();
        } else {
            throw new IllegalStateException("No transaction is active");
        }
    }
}
```

### 스프링 부트 적용 방법

스프링 부트 애플리케이션에서는 다음과 같이 Bean으로 등록합니다.

```java
@Configuration
public class TransactionConfig {
    
    @Bean
    public TransactionManager eventifyTransactionManager(PlatformTransactionManager platformTransactionManager) {
        // Spring의 PlatformTransactionManager를 어댑터로 감싸서 eventify가 사용할 수 있게 함
        return new SpringTransactionManagerAdapter(platformTransactionManager);
    }
}
```

---

## 2. Jakarta Persistence (JTA) 환경용 어댑터

Jakarta Persistence 환경(JTA를 사용하는 container-managed 환경)에서는, JTA의 `UserTransaction`이나 애플리케이션 서버가 제공하는 `TransactionManager`를 사용합니다.
JPA 표준 자체는 범용 트랜잭션 매니저 인터페이스를 제공하지 않으므로,
사용자는 JTA 표준을 활용하여 어댑터를 작성해야 합니다.

### 예제: JakartaTransactionManagerAdapter

```java
package io.github.event.transaction;

import jakarta.transaction.UserTransaction;

public class JakartaTransactionManagerAdapter implements TransactionManager {
    private final UserTransaction userTransaction;

    public JakartaTransactionManagerAdapter(UserTransaction userTransaction) {
        this.userTransaction = userTransaction;
    }

    @Override
    public void begin() {
        try {
            userTransaction.begin();
        } catch (Exception e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    @Override
    public void commit() {
        try {
            userTransaction.commit();
        } catch (Exception e) {
            throw new RuntimeException("Failed to commit transaction", e);
        }
    }

    @Override
    public void rollback() {
        try {
            userTransaction.rollback();
        } catch (Exception e) {
            throw new RuntimeException("Failed to rollback transaction", e);
        }
    }
}
```

### Jakarta EE 적용 방법

- Jakarta EE 환경에서는 보통 JNDI 등을 통해 `UserTransaction`을 획득합니다.
- 획득한 `UserTransaction`을 이용해 위 어댑터를 생성하고, eventify의 트랜잭션 매니저로 주입합니다.
- 컨테이너나 CDI를 통해 어댑터 Bean을 등록할 수 있습니다.

---

## 결론

- **Spring Boot/JPA 환경에서는** `SpringTransactionManagerAdapter`를 사용하여 스프링의 `PlatformTransactionManager`를 eventify에 주입할 수 있습니다.
- **Jakarta Persistence/JTA 환경에서는** JTA의 `UserTransaction` 등을 활용한 `JakartaTransactionManagerAdapter`를 작성하여 동일한 역할을 수행할 수 있습니다.

어댑터를 작성하여 eventify의 TransactionManager 인터페이스에 맞게 환경에 따른 트랜잭션 관리자를 주입하면,
트랜잭션 연동 이벤트 처리 기능을 원활하게 사용할 수 있습니다. 