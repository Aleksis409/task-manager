package com.javarush.taskmanager.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    public SecurityUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
            throw new AccessDeniedException("Unauthorized");
        }

        return securityUser;
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}

