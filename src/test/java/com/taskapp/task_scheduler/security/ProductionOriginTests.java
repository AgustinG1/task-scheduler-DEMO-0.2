package com.taskapp.task_scheduler.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;

import static org.assertj.core.api.Assertions.*;

class ProductionOriginTests {
    @Test
    void productionLogoutUsesTheConfiguredHttpsOriginAndLocalKeepsDynamicBaseUrl() {
        assertThat(WebSecurityConfiguration.postLogoutRedirect("https://portfolio.example.test", true))
                .isEqualTo("https://portfolio.example.test/");
        assertThat(WebSecurityConfiguration.postLogoutRedirect("", false))
                .isEqualTo("{baseUrl}/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://portfolio.example.test", "https://user:password@portfolio.example.test",
            "https://portfolio.example.test/", "https://portfolio.example.test/path",
            "https://portfolio.example.test?returnTo=elsewhere", "https://portfolio.example.test#fragment",
            "https://portfolio.example.test:8080", "//portfolio.example.test", "https://"})
    void invalidPublicOriginCannotBecomeALogoutDestination(String value) {
        assertThatThrownBy(() -> WebSecurityConfiguration.postLogoutRedirect(value, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyProductionOriginIsRejectedInsteadOfUsingTheIncomingHost() {
        assertThatThrownBy(() -> WebSecurityConfiguration.postLogoutRedirect("", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void localAuth0ConfigurationKeepsDynamicCallbackWithoutRequiringPublicUrl() {
        // Use the application's ConfigData startup path, with no auto-configured
        // datasource/provider beans. This tests the local launcher's profile pair.
        try (var context = new SpringApplicationBuilder(ProfilesOnly.class).web(WebApplicationType.NONE)
                .run("--spring.profiles.active=local,auth0", "--spring.main.banner-mode=off")) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("local", "auth0");
            assertThat(context.getEnvironment().getProperty("spring.security.oauth2.client.registration.auth0.redirect-uri"))
                    .isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProfilesOnly { }
}
