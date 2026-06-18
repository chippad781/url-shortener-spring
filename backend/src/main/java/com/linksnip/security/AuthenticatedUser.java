package com.linksnip.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

/**
 * Spring Security principal. We extend the framework's User so existing
 * machinery works, but carry the database id so controllers can resolve the
 * owner without an extra lookup.
 */
public class AuthenticatedUser extends User {

    private final Long id;

    public AuthenticatedUser(Long id, String email, String passwordHash) {
        super(email, passwordHash, AuthorityUtils.NO_AUTHORITIES);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return getUsername();
    }
}
