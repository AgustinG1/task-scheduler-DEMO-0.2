package com.taskapp.auth;

import java.util.Optional;

import com.taskapp.auth.autoconfigure.PersonalAuthAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalAuthAutoConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PersonalAuthAutoConfiguration.class));

    @Test
    void providesIdentityAccessWithoutTakingOverHttpSecurity() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(CurrentUser.class);
            assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
        });
    }

    @Test
    void usesTheConsumingApplicationsImplementationWhenProvided() {
        runner.withUserConfiguration(CustomIdentityConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(CurrentUser.class);
            assertThat(context.getBean(CurrentUser.class)).isSameAs(context.getBean("customCurrentUser"));
            assertThat(context).doesNotHaveBean(SecurityContextCurrentUser.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomIdentityConfiguration {
        @Bean
        CurrentUser customCurrentUser() {
            return Optional::empty;
        }
    }
}
