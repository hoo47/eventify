package io.github.event.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import io.github.event.core.api.Event;
import io.github.event.core.api.EventListener;
import io.github.event.core.model.EventHeaders;
import io.github.event.core.model.TransactionPhase;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Getter
public class RabbitMQConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConfig.class);
    
    private final RabbitMQConnectionManager connectionManager;
    private final String exchangeName;
    private final String queueName;
    
    public RabbitMQConfig(
            RabbitMQConnectionManager connectionManager,
            String exchangeName,
            String queueName) {
        this.connectionManager = connectionManager;
        this.exchangeName = exchangeName;
        this.queueName = queueName;
    }
    
    public void initialize() throws IOException, TimeoutException {
        try (Channel channel = connectionManager.createChannel()) {
            // 교환기 선언
            channel.exchangeDeclare(exchangeName, "topic", true);
            log.info("RabbitMQ 교환기 선언됨: {}", exchangeName);
            
            // 데드 레터 교환기 설정
            String dlxName = exchangeName + ".dlx";
            channel.exchangeDeclare(dlxName, "topic", true);
            
            // 큐 설정
            Map<String, Object> args = new HashMap<>();
            args.put("x-dead-letter-exchange", dlxName);
            
            channel.queueDeclare(queueName, true, false, false, args);
            channel.queueBind(queueName, exchangeName, "#");
            log.info("RabbitMQ 큐 선언 및 바인딩됨: {}", queueName);
            
            // 데드 레터 큐 설정
            String dlqName = queueName + ".dlq";
            channel.queueDeclare(dlqName, true, false, false, null);
            channel.queueBind(dlqName, dlxName, "#");
            log.info("RabbitMQ 데드 레터 큐 설정됨: {}", dlqName);
            
            // 트랜잭션 단계별 큐 설정
            for (TransactionPhase phase : TransactionPhase.values()) {
                String queueName = this.queueName + "." + phase.name().toLowerCase();
                String routingKey = "async." + phase.name().toLowerCase() + ".*";
                
                channel.queueDeclare(queueName, true, false, false, args);
                channel.queueBind(queueName, exchangeName, routingKey);
                log.info("RabbitMQ 트랜잭션 단계 큐 선언 및 바인딩됨: {}, 라우팅 키: {}", queueName, routingKey);
            }
        }
    }
}
