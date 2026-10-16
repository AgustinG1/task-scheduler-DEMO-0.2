package com.taskapp.auth;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Reads Spring Security's validated principal on every invocation; never caches a user. */
public final class SecurityContextCurrentUser implements CurrentUser {

    @Override
    public Optional<UserIdentity> find() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken token)
                || !token.isAuthenticated()
                || !(token.getPrincipal() instanceof OidcUser user)) {
            return Optional.empty();
        }

        var idToken = user.getIdToken();
        var identity = new ExternalUserId(idToken.getIssuer().toExternalForm(), idToken.getSubject());
        return Optional.of(new UserIdentity(identity, user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()), user.getFullName()));
    }
}
