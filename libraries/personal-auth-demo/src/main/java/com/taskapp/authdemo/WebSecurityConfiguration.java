package com.taskapp.authdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class WebSecurityConfiguration {

    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http, ClientRegistrationRepository registrations) throws Exception {
        var logout = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        logout.setPostLogoutRedirectUri("{baseUrl}/");
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error", "/assets/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login.defaultSuccessUrl("/profile", true))
                .logout(config -> config.logoutSuccessHandler(logout))
                // Keep Spring's CSRF, session fixation and response-header protections.
                .build();
    }
}
