package com.microservices.auth.strategy;

import com.microservices.auth.entity.User;
import com.microservices.auth.model.AuthType;

public interface AuthenticationStrategy {
    boolean supports(AuthType authType);
    User authenticate(String email, String credentials);
}