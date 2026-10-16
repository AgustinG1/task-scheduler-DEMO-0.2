package com.taskapp.auth;

import java.util.Objects;

/** Profile claims from an already authenticated OIDC principal; contains no tokens. */
public record UserIdentity(
        ExternalUserId id, String email, boolean emailVerified, String displayName) {

    public UserIdentity {
        Objects.requireNonNull(id, "External identity is required");
    }

    public boolean hasVerifiedEmail() {
        return emailVerified && email != null && !email.isBlank();
    }
}
