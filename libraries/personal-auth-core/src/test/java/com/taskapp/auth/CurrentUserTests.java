package com.taskapp.auth;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTests {

    private final CurrentUser currentUser = new SecurityContextCurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingAuthentication() {
        assertThat(currentUser.find()).isEmpty();
        assertThatThrownBy(currentUser::require)
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void rejectsAnonymousAndNonOidcAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(currentUser.find()).isEmpty();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", null,
                        AuthorityUtils.createAuthorityList("ROLE_USER")));
        assertThat(currentUser.find()).isEmpty();
    }

    @Test
    void rejectsOAuthWithoutAnOidcPrincipal() {
        var principal = new DefaultOAuth2User(AuthorityUtils.createAuthorityList("OAUTH2_USER"),
                Map.of("id", "test-user"), "id");
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "test"));
        assertThat(currentUser.find()).isEmpty();
    }

    @Test
    void rejectsAnOidcTokenMarkedUnauthenticated() {
        var token = signIn("https://issuer.example.test/", "user-a", "a@example.test", true);
        token.setAuthenticated(false);
        assertThat(currentUser.find()).isEmpty();
    }

    @Test
    void readsIdentityFromTheAuthenticatedPrincipal() {
        signIn("https://issuer.example.test/", "user-a", "a@example.test", true);
        UserIdentity identity = currentUser.requireVerifiedEmail();
        assertThat(identity.id()).isEqualTo(new ExternalUserId("https://issuer.example.test/", "user-a"));
        assertThat(identity.email()).isEqualTo("a@example.test");
        assertThat(identity.displayName()).isEqualTo("Test User");
    }

    @Test
    void doesNotCacheTheIdentityBetweenUsersOrAfterLogout() {
        signIn("https://issuer.example.test/", "user-a", "a@example.test", true);
        assertThat(currentUser.require().id().subject()).isEqualTo("user-a");
        signIn("https://issuer.example.test/", "user-b", "b@example.test", true);
        assertThat(currentUser.require().id().subject()).isEqualTo("user-b");
        SecurityContextHolder.clearContext();
        assertThat(currentUser.find()).isEmpty();
    }

    @Test
    void issuerAndSubjectDistinguishUsersWithTheSameEmail() {
        signIn("https://issuer-a.example.test/", "user-a", "shared@example.test", true);
        ExternalUserId first = currentUser.require().id();
        signIn("https://issuer-a.example.test/", "user-b", "shared@example.test", true);
        ExternalUserId second = currentUser.require().id();
        signIn("https://issuer-b.example.test/", "user-a", "shared@example.test", true);
        ExternalUserId third = currentUser.require().id();
        assertThat(first).isNotEqualTo(second).isNotEqualTo(third);
    }

    @Test
    void changingTheEmailDoesNotChangeTheIdentity() {
        signIn("https://issuer.example.test/", "user-a", "old@example.test", true);
        ExternalUserId first = currentUser.require().id();
        signIn("https://issuer.example.test/", "user-a", "new@example.test", true);
        assertThat(currentUser.require().id()).isEqualTo(first);
    }

    @Test
    void unverifiedOrMissingVerificationCannotPassTheEmailPolicy() {
        signIn("https://issuer.example.test/", "user-a", "a@example.test", false);
        assertThatThrownBy(currentUser::requireVerifiedEmail).isInstanceOf(AccessDeniedException.class);
        signIn("https://issuer.example.test/", "user-a", "a@example.test", null);
        assertThatThrownBy(currentUser::requireVerifiedEmail).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void verifiedFlagWithoutAnEmailCannotPassTheEmailPolicy() {
        CurrentUser noEmail = () -> Optional.of(new UserIdentity(
                new ExternalUserId("https://issuer.example.test/", "user-a"), null, true, null));
        assertThatThrownBy(noEmail::requireVerifiedEmail).isInstanceOf(AccessDeniedException.class);
        CurrentUser blankEmail = () -> Optional.of(new UserIdentity(
                new ExternalUserId("https://issuer.example.test/", "user-a"), " ", true, null));
        assertThatThrownBy(blankEmail::requireVerifiedEmail).isInstanceOf(AccessDeniedException.class);
    }

    private OAuth2AuthenticationToken signIn(String issuer, String subject, String email, Boolean verified) {
        Instant now = Instant.now();
        var builder = OidcIdToken.withTokenValue("synthetic-test-token")
                .issuer(issuer).subject(subject).issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("email", email).claim("name", "Test User");
        if (verified != null) {
            builder.claim("email_verified", verified);
        }
        var principal = new DefaultOidcUser(AuthorityUtils.createAuthorityList("OIDC_USER"), builder.build());
        var token = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "test");
        SecurityContextHolder.getContext().setAuthentication(token);
        return token;
    }
}
