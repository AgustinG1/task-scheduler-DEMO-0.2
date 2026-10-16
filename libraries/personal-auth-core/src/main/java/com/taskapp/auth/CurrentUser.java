package com.taskapp.auth;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/** Request-time identity access. Each consuming app resolves its own local user and permissions. */
public interface CurrentUser {

    Optional<UserIdentity> find();

    default UserIdentity require() {
        return find().orElseThrow(() ->
                new AuthenticationCredentialsNotFoundException("OIDC authentication is required"));
    }

    default UserIdentity requireVerifiedEmail() {
        UserIdentity identity = require();
        if (!identity.hasVerifiedEmail()) {
            throw new AccessDeniedException("A verified email is required");
        }
        return identity;
    }
}
