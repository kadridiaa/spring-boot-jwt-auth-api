package com.microservices.notification.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.queue.userRegistered}")
    private String userRegisteredQueue;

    @Value("${app.mq.rk.userRegistered}")
    private String userRegisteredRoutingKey;

    @Value("${app.mq.dlq.exchange}")
    private String dlqExchange;

    @Value("${app.mq.dlq.queue}")
    private String dlqQueue;

    @Bean
    public TopicExchange authEventsExchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(dlqExchange);
    }

    @Bean
    public Queue userRegisteredQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlqExchange);
        args.put("x-dead-letter-routing-key", "dlq." + userRegisteredRoutingKey);
        return new Queue(userRegisteredQueue, true, false, false, args);
    }

    @Bean
    public Queue dlqQueue() {
        return new Queue(dlqQueue, true);
    }

    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder
            .bind(userRegisteredQueue())
            .to(authEventsExchange())
            .with(userRegisteredRoutingKey);
    }

    @Bean
    public Binding dlqBinding() {
        return BindingBuilder
            .bind(dlqQueue())
            .to(dlqExchange())
            .with("dlq." + userRegisteredRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setDefaultRequeueRejected(false); // Don't requeue on error, send to DLQ
        return factory;
    }
}
