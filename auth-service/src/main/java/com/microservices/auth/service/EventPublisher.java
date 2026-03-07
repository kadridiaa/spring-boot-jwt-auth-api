package com.microservices.auth.service;

import com.microservices.auth.event.EmailVerifiedEvent;
import com.microservices.auth.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.mq.exchange}")
    private String exchange;

    @Value("${app.mq.rk.userRegistered}")
    private String userRegisteredRoutingKey;

    @Value("${app.mq.rk.emailVerified}")
    private String emailVerifiedRoutingKey;

    // Constructeur pour injection de dépendances
    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent: eventId={}, userId={}, tokenId={}", 
            event.getEventId(), event.getUserId(), event.getTokenId());
        
        rabbitTemplate.convertAndSend(exchange, userRegisteredRoutingKey, event, message -> {
            message.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
            message.getMessageProperties().setHeader("x-schema-version", event.getSchemaVersion());
            return message;
        });
        
        log.info("UserRegisteredEvent published successfully");
    }

    public void publishEmailVerified(EmailVerifiedEvent event) {
        log.info("Publishing EmailVerifiedEvent: eventId={}, userId={}", 
            event.getEventId(), event.getUserId());
        
        rabbitTemplate.convertAndSend(exchange, emailVerifiedRoutingKey, event, message -> {
            message.getMessageProperties().setHeader("x-correlation-id", event.getCorrelationId());
            message.getMessageProperties().setHeader("x-schema-version", event.getSchemaVersion());
            return message;
        });
        
        log.info("EmailVerifiedEvent published successfully");
    }
}
