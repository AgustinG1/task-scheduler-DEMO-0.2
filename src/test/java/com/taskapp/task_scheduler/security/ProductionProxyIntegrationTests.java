package com.taskapp.task_scheduler.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Load the real prod/auth0 documents; all database properties are explicitly H2.
// The synthetic provider never calls Auth0 or exchanges a real token.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:prod_proxy;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.sql.init.mode=never",
        "server.address=127.0.0.1", "server.servlet.session.cookie.secure=true",
        "APP_PUBLIC_URL=https://task-scheduler-app-demo-0-2.onrender.com",
        "AUTH0_CLIENT_ID=synthetic-proxy-client", "AUTH0_CLIENT_SECRET=synthetic-only",
        "AUTH0_ISSUER=https://idp.example.test/"
})
@ActiveProfiles({"local", "prod", "auth0"})
@AutoConfigureMockMvc
@Import(ProductionProxyIntegrationTests.Provider.class)
class ProductionProxyIntegrationTests {
    private static final String PUBLIC_ORIGIN = "https://task-scheduler-app-demo-0-2.onrender.com";
    @LocalServerPort int port;
    @Autowired MockMvc http;
    @Autowired Environment environment;
    @Autowired ClientRegistrationRepository registrations;

    @Test
    void actualServletBehindProxySetsSecureSessionAndUsesCanonicalCallbackEvenWithAnotherHost() throws Exception {
        assertThat(environment.getProperty("server.forward-headers-strategy")).isEqualTo("framework");
        assertThat(environment.getProperty("server.tomcat.redirect-context-root")).isEqualTo("false");
        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10)).build();
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/oauth2/authorization/auth0"))
                .header("Forwarded", "for=192.0.2.1;proto=https;host=untrusted.example.test")
                .timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(302);
        var location = response.headers().firstValue("Location").orElseThrow();
        var query = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(java.net.URLDecoder.decode(query.getFirst("redirect_uri"), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo(PUBLIC_ORIGIN + "/login/oauth2/code/auth0");
        assertThat(query.getFirst("response_type")).isEqualTo("code");
        assertThat(query.getFirst("state")).isNotBlank();
        assertThat(query.getFirst("nonce")).isNotBlank();
        assertThat(response.headers().allValues("Set-Cookie")).anySatisfy(cookie ->
                assertThat(cookie).contains("TASKSCHEDULER_SESSION=", "Secure", "HttpOnly", "SameSite=Lax"));
    }

    @Test
    void logoutUsesCanonicalHttpsReturnUriAndRequiresCsrf() throws Exception {
        var identity = oidcLogin().clientRegistration(registrations.findByRegistrationId("auth0"));
        http.perform(post("/logout").with(identity)).andExpect(status().isForbidden());
        var response = http.perform(post("/logout").with(identity).with(csrf())
                        .header("Forwarded", "proto=https;host=untrusted.example.test"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse();
        var query = UriComponentsBuilder.fromUriString(response.getHeader("Location")).build().getQueryParams();
        assertThat(java.net.URLDecoder.decode(query.getFirst("post_logout_redirect_uri"), java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo(PUBLIC_ORIGIN + "/");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Provider {
        @Bean
        ClientRegistrationRepository syntheticProvider(Environment environment) {
            // Preserve the actual redirect URI read from prod+auth0 configuration.
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("auth0")
                    .clientId("synthetic-proxy-client").clientSecret("synthetic-only")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(environment.getRequiredProperty("spring.security.oauth2.client.registration.auth0.redirect-uri"))
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://idp.example.test/authorize")
                    .tokenUri("https://idp.example.test/token").jwkSetUri("https://idp.example.test/jwks")
                    .userInfoUri("https://idp.example.test/userinfo").userNameAttributeName("sub")
                    .providerConfigurationMetadata(Map.of("end_session_endpoint", "https://idp.example.test/logout"))
                    .clientName("Synthetic provider").build());
        }
    }
}
