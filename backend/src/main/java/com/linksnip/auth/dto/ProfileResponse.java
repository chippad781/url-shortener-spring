package com.linksnip.auth.dto;

import com.linksnip.user.User;

import java.time.Instant;

public record ProfileResponse(Long id, String email, String displayName, Instant createdAt) {
    public static ProfileResponse from(User u) {
        return new ProfileResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getCreatedAt());
    }
}
