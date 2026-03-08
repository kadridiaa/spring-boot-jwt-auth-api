package com.microservices.notification.consumer;

import com.microservices.notification.event.UserRegisteredEvent;
import com.microservices.notification.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisteredConsumer.class);

    private final EmailService emailService;

    public UserRegisteredConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.mq.queue.userRegistered}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: eventId={}, userId={}, email={}", 
            event.getEventId(), event.getUserId(), event.getEmail());
        
        try {
            // Send verification email
            emailService.sendVerificationEmail(
                event.getEmail(),
                event.getTokenId(),
                event.getTokenClear()
            );
            
            log.info("Verification email sent successfully to {}", maskEmail(event.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", 
                maskEmail(event.getEmail()), e.getMessage(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String maskedLocal = localPart.substring(0, Math.min(2, localPart.length())) + "***";
        return maskedLocal + "@" + parts[1];
    }
}
