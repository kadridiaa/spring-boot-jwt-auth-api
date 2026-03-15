package com.microservices.auth.security;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class PermissionValidator {

    private static final String SUPER_ADMIN_PERMISSION = "ALL_ACCESS";

    public boolean hasPermission(Set<String> userPermissions, String requiredPermission) {
        if (userPermissions == null || userPermissions.isEmpty()) {
            return false;
        }
        if (userPermissions.contains(SUPER_ADMIN_PERMISSION)) {
            return true;
        }
        return userPermissions.contains(requiredPermission);
    }
}