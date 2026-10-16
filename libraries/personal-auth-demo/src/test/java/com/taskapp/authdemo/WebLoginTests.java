package com.taskapp.authdemo;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(WebLoginTests.TestProvider.class)
class WebLoginTests {
    @Autowired MockMvc http;
    @Autowired ClientRegistrationRepository registrations;

    @Test
    void publicHomeRendersWithoutAuthentication() throws Exception {
        http.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Entrar o crear una cuenta")));
    }

    @Test
    void privateProfileRequiresLogin() throws Exception {
        http.perform(get("/api/me")).andExpect(status().is3xxRedirection());
        http.perform(get("/profile")).andExpect(status().is3xxRedirection());
    }

    @Test
    void authorizationStartsACodeFlowWithStateAndNonce() throws Exception {
        http.perform(get("/oauth2/authorization/auth0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("response_type=code")))
                .andExpect(header().string("Location", containsString("state=")))
                .andExpect(header().string("Location", containsString("nonce=")));
    }

    @Test
    void verifiedUserSeesOnlyProfileFields() throws Exception {
        http.perform(get("/api/me").with(user("a", true)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("a@example.test"))
                .andExpect(jsonPath("$.displayName").value("User a"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.idToken").doesNotExist());
        http.perform(get("/profile").with(user("a", true)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("a@example.test")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void unverifiedEmailCannotAccessEitherProfileRoute() throws Exception {
        http.perform(get("/api/me").with(user("a", false))).andExpect(status().isForbidden());
        http.perform(get("/profile").with(user("a", false))).andExpect(status().isForbidden());
    }

    @Test
    void missingVerificationDoesNotGrantAccess() throws Exception {
        http.perform(get("/api/me").with(user("a", null))).andExpect(status().isForbidden());
    }

    @Test
    void twoSessionsKeepTheirOwnIdentityAcrossRequests() throws Exception {
        MockHttpSession a = session("a");
        MockHttpSession b = session("b");
        http.perform(get("/api/me").session(a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@example.test"));
        http.perform(get("/api/me").session(b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("b@example.test"));
        http.perform(get("/api/me").session(a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@example.test"));
    }

    @Test
    void logoutWithoutCsrfCannotTerminateTheSession() throws Exception {
        MockHttpSession a = session("a");
        http.perform(post("/logout").session(a)).andExpect(status().isForbidden());
        http.perform(get("/api/me").session(a)).andExpect(status().isOk());
    }

    @Test
    void logoutEndsOnlyTheCurrentSessionAndRedirectsToTheProvider() throws Exception {
        MockHttpSession a = session("a");
        MockHttpSession b = session("b");
        http.perform(post("/logout").session(a).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://idp.example.test/logout")))
                .andExpect(header().string("Location", containsString("post_logout_redirect_uri=")));
        assertThat(a.isInvalid()).isTrue();
        http.perform(get("/api/me")).andExpect(status().is3xxRedirection());
        http.perform(get("/api/me").session(b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("b@example.test"));
    }

    @Test
    void getLogoutDoesNotMutateTheSession() throws Exception {
        MockHttpSession a = session("a");
        http.perform(get("/logout").session(a)).andExpect(status().isOk());
        http.perform(get("/api/me").session(a)).andExpect(status().isOk());
    }

    private MockHttpSession session(String name) throws Exception {
        var session = new MockHttpSession();
        http.perform(get("/api/me").session(session).with(user(name, true))).andExpect(status().isOk());
        return session;
    }

    private OidcLoginRequestPostProcessor user(String name, Boolean verified) {
        return oidcLogin().clientRegistration(registrations.findByRegistrationId("auth0"))
                .idToken(token -> {
                    token.issuer("https://idp.example.test/").subject("user-" + name)
                            .claim("email", name + "@example.test").claim("name", "User " + name);
                    if (verified != null) {
                        token.claim("email_verified", verified);
                    }
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestProvider {
        @Bean
        ClientRegistrationRepository registrations() {
            var registration = ClientRegistration.withRegistrationId("auth0")
                    .clientId("synthetic-test-client").clientSecret("synthetic-test-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .scope("openid", "profile", "email")
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("https://idp.example.test/authorize")
                    .tokenUri("https://idp.example.test/token")
                    .jwkSetUri("https://idp.example.test/jwks")
                    .userInfoUri("https://idp.example.test/userinfo").userNameAttributeName("sub")
                    .issuerUri("https://idp.example.test/")
                    .providerConfigurationMetadata(Map.of("end_session_endpoint", "https://idp.example.test/logout"))
                    .clientName("Test Provider").build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }
}
