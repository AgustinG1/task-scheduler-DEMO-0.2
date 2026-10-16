package com.taskapp.task_scheduler.security;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.taskapp.task_scheduler.model.Area;
import com.taskapp.task_scheduler.repository.AppUserRepository;
import com.taskapp.task_scheduler.repository.AreaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:accounts;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Import(AccountIntegrationTests.TestProvider.class)
public class AccountIntegrationTests {
    @Autowired MockMvc http;
    @Autowired ClientRegistrationRepository registrations;
    @Autowired AppUserRepository users;
    @Autowired AreaRepository areas;

    @BeforeEach
    void resetAccounts() {
        users.deleteAll();
    }

    @Test
    void publicHomeContainsLoginButNoPrivateCounts() throws Exception {
        http.perform(get("/")).andExpect(status().isOk())
                .andExpect(view().name("account/index"))
                .andExpect(content().string(containsString("Entrar o crear una cuenta")))
                .andExpect(model().attributeDoesNotExist("areasCount", "employeesCount", "payrollsCount"));
        http.perform(get("/assets/account.css")).andExpect(status().isOk());
        http.perform(get("/account")).andExpect(status().is3xxRedirection());
        http.perform(get("/api/me")).andExpect(status().is3xxRedirection());
        assertThat(users.count()).isZero();
    }

    @Test
    void authorizationUsesCodeStateAndNonceAndRejectsAnUnsolicitedCallback() throws Exception {
        http.perform(get("/oauth2/authorization/auth0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(containsString("response_type=code"),
                        containsString("state="), containsString("nonce="))));
        http.perform(get("/login/oauth2/code/auth0").param("code", "not-a-real-code").param("state", "unknown"))
                .andExpect(redirectedUrl("/?loginError"));
        assertThat(users.count()).isZero();
    }

    @Test
    void repeatedAccessKeepsLocalIdAndUpdatesOnlyProfile() throws Exception {
        String first = accountId("A", "first@example.test", "Alice");
        String second = accountId("A", "changed@example.test", "Changed");
        assertThat(second).isEqualTo(first);
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findAll().get(0).getEmail()).isEqualTo("changed@example.test");
        http.perform(get("/api/me").with(identity("A", "changed@example.test", "Changed", true)))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.idToken").doesNotExist())
                .andExpect(jsonPath("$.issuer").doesNotExist());
    }

    @Test
    void sharedEmailAndSubjectCaseDoNotMergeIdentities() throws Exception {
        String a = accountId("A", "shared@example.test", "A");
        String b = accountId("a", "shared@example.test", "B");
        assertThat(a).isNotEqualTo(b);
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void sameSubjectAtAnotherIssuerIsAnotherAccount() throws Exception {
        accountId("A", "shared@example.test", "A");
        http.perform(get("/api/me").with(oidcLogin()
                .clientRegistration(registrations.findByRegistrationId("auth0"))
                .idToken(t -> t.issuer("https://other.example.test/").subject("A")
                        .claim("email", "shared@example.test").claim("email_verified", true))))
                .andExpect(status().isOk());
        assertThat(users.count()).isEqualTo(2);
    }

    @Test
    void unverifiedOrMissingEmailCannotCreateLocalAccount() throws Exception {
        for (Boolean verified : new Boolean[]{false, null}) {
            http.perform(get("/api/me").with(identity("A", "a@example.test", "A", verified)))
                    .andExpect(status().isForbidden());
            http.perform(get("/account").with(identity("A", "a@example.test", "A", verified)))
                    .andExpect(status().isForbidden()).andExpect(view().name("account/verify-email"))
                    .andExpect(content().string(containsString("name=\"_csrf\"")));
        }
        http.perform(get("/api/me").with(identity("A", null, "A", true)))
                .andExpect(status().isForbidden());
        assertThat(users.count()).isZero();
    }

    @Test
    void aNonOidcPrincipalCannotBecomeALocalAccount() throws Exception {
        http.perform(get("/api/me").with(user("local-user"))).andExpect(status().is3xxRedirection());
        assertThat(users.count()).isZero();
    }

    @Test
    void profileEscapesClaimsAndContainsLogoutCsrf() throws Exception {
        http.perform(get("/account").with(identity("A", "a@example.test", "<script>alert(1)</script>", true)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Abrir mi panel")));
    }

    @Test
    void deactivatedAccountIsDeniedEvenWithItsExistingSession() throws Exception {
        var session = session("A");
        var account = users.findAll().get(0);
        account.deactivate();
        users.saveAndFlush(account);
        http.perform(get("/api/me").session(session)).andExpect(status().isForbidden());
        http.perform(get("/account").session(session)).andExpect(status().isForbidden());
        assertThat(users.findById(account.getId()).orElseThrow().isActive()).isFalse();
        http.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/app", "/areas", "/tasks", "/employees", "/task-groups", "/teams",
            "/payroll", "/payroll/historial"})
    void businessRoutesRequireAVerifiedAccount(String path) throws Exception {
        http.perform(get(path)).andExpect(status().is3xxRedirection());
        http.perform(get(path).with(identity("A", "a@example.test", "A", true)))
                .andExpect(status().isOk());
        http.perform(get(path).with(identity("U", "u@example.test", "U", false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void firstLoginDoesNotAdoptHistoricalDataAndOwnerInputIsIgnored() throws Exception {
        var area = new Area();
        area.setNombre("Historical fixture");
        area = areas.saveAndFlush(area);
        try {
            accountId("first-user", "a@example.test", "A");
            http.perform(get("/areas/eliminar/" + area.getId()).with(identity("A", "a@example.test", "A", true)))
                    .andExpect(status().isMethodNotAllowed());
            http.perform(post("/areas/eliminar/" + area.getId()).with(csrf()).with(identity("A", "a@example.test", "A", true)))
                    .andExpect(status().isNotFound());
            http.perform(post("/areas/guardar").with(csrf()).with(identity("A", "a@example.test", "A", true))
                            .param("nombre", "Injected").param("ownerId", "1"))
                    .andExpect(status().is3xxRedirection());
            assertThat(areas.findById(area.getId()).orElseThrow().getNombre()).isEqualTo("Historical fixture");
            Long actualOwner = users.findAll().stream().filter(u -> "A".equals(u.getSubject()))
                    .findFirst().orElseThrow().getId();
            assertThat(areas.findAll().stream().filter(a -> "Injected".equals(a.getNombre())))
                    .allMatch(a -> a.getOwner() != null && actualOwner.equals(a.getOwner().getId()));
        } finally {
            areas.deleteById(area.getId());
            areas.findAll().stream().filter(a -> "Injected".equals(a.getNombre())).forEach(areas::delete);
        }
    }

    @Test
    void twoSessionsStaySeparateAndLogoutRequiresCsrf() throws Exception {
        var a = session("A");
        var b = session("B");
        http.perform(post("/logout").session(a)).andExpect(status().isForbidden());
        http.perform(get("/api/me").session(a)).andExpect(jsonPath("$.email").value("A@example.test"));
        // With a custom login page there is no generated GET logout confirmation page.
        http.perform(get("/logout").session(a)).andExpect(status().isForbidden());
        assertThat(a.isInvalid()).isFalse();
        http.perform(get("/api/me").session(a)).andExpect(status().isOk());
        http.perform(post("/logout").session(a).with(csrf())).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("https://idp.example.test/logout")));
        assertThat(a.isInvalid()).isTrue();
        http.perform(get("/api/me")).andExpect(status().is3xxRedirection());
        http.perform(get("/api/me").session(b)).andExpect(jsonPath("$.email").value("B@example.test"));
    }

    @Test
    void simultaneousFirstRequestsCreateOneAccount() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            var jobs = java.util.stream.IntStream.range(0, 2).mapToObj(i -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                return accountId("concurrent", "same@example.test", "Same");
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(jobs.get(0).get(15, TimeUnit.SECONDS)).isEqualTo(jobs.get(1).get(15, TimeUnit.SECONDS));
            assertThat(users.count()).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private String accountId(String subject, String email, String name) throws Exception {
        var result = http.perform(get("/api/me").with(identity(subject, email, name, true)))
                .andExpect(status().isOk()).andReturn();
        // Comparing the id alone also proves that a changed email did not create another account.
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();
    }

    private MockHttpSession session(String subject) throws Exception {
        var session = new MockHttpSession();
        http.perform(get("/api/me").session(session).with(identity(subject, subject + "@example.test", subject, true)))
                .andExpect(status().isOk());
        return session;
    }

    private OidcLoginRequestPostProcessor identity(String subject, String email, String name, Boolean verified) {
        return oidcLogin().clientRegistration(registrations.findByRegistrationId("auth0"))
                .idToken(token -> {
                    token.issuer("https://idp.example.test/").subject(subject);
                    if (email != null) token.claim("email", email);
                    if (name != null) token.claim("name", name);
                    if (verified != null) token.claim("email_verified", verified);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class TestProvider {
        @Bean
        ClientRegistrationRepository registrations() {
            var registration = ClientRegistration.withRegistrationId("auth0")
                    .clientId("synthetic-test-client").clientSecret("synthetic-test-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .scope("openid", "profile", "email")
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("https://idp.example.test/authorize")
                    .tokenUri("https://idp.example.test/token").jwkSetUri("https://idp.example.test/jwks")
                    .userInfoUri("https://idp.example.test/userinfo").userNameAttributeName("sub")
                    .issuerUri("https://idp.example.test/")
                    .providerConfigurationMetadata(Map.of("end_session_endpoint", "https://idp.example.test/logout"))
                    .clientName("Test provider").build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }
}
