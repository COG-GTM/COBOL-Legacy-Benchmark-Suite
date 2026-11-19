package com.clbs.portfolio.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Replaces {@code EXEC CICS ASSIGN USERID} / {@code FUNCTION USER-ID}. */
@Component
public class CurrentUser {

    static final String BATCH_USER = "BATCH";

    public String name() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth.getName() == null ? BATCH_USER : auth.getName();
    }
}
