package com.microservices.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.verification.url}")
    private String verificationUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String tokenId, String tokenClear) {
        log.info("Preparing verification email for {}", toEmail);

        // Build verification link
        String verificationLink = String.format("%s?tokenId=%s&t=%s", 
            verificationUrl, tokenId, tokenClear);

        // Create email message
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your email address");
        message.setText(buildEmailContent(verificationLink));

        // Send email
        try {
            mailSender.send(message);
            log.info("Verification email sent to {} (via SMTP)", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private String buildEmailContent(String verificationLink) {
        return """
            Welcome to our Microservices Platform!
            
            Thank you for registering. Please verify your email address by clicking the link below:
            
            %s
            
            This link will expire in 30 minutes.
            
            If you didn't create an account, please ignore this email.
            
            Best regards,
            The Microservices Team
            """.formatted(verificationLink);
    }
}
