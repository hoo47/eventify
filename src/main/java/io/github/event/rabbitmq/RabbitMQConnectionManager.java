package io.github.event.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQConnectionManager.class);
    
    private final ConnectionFactory factory;
    private Connection connection;
    
    public RabbitMQConnectionManager(String host, int port, String username, String password) {
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
    }
    
    public synchronized Connection getConnection() throws IOException, TimeoutException {
        if (connection == null || !connection.isOpen()) {
            try {
                connection = factory.newConnection();
                log.info("RabbitMQ 연결 생성됨: {}:{}", factory.getHost(), factory.getPort());
            } catch (IOException | TimeoutException e) {
                log.error("RabbitMQ 연결 실패: {}", e.getMessage(), e);
                throw e;
            }
        }
        return connection;
    }
    
    public Channel createChannel() throws IOException, TimeoutException {
        try {
            Channel channel = getConnection().createChannel();
            log.debug("RabbitMQ 채널 생성됨");
            return channel;
        } catch (IOException e) {
            log.error("RabbitMQ 채널 생성 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    public void close() {
        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
                log.info("RabbitMQ 연결 종료됨");
            } catch (IOException e) {
                log.error("RabbitMQ 연결 종료 실패: {}", e.getMessage(), e);
            }
        }
    }
} 