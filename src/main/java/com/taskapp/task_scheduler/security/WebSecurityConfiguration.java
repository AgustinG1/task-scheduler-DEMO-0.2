package com.taskapp.task_scheduler.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebSecurityConfiguration {
    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> provider, AppUserService accounts,
            @Value("${app.public-base-url:}") String publicOrigin, Environment environment) throws Exception {
        String logoutRedirect = postLogoutRedirect(publicOrigin, environment.acceptsProfiles(Profiles.of("prod")));
        ClientRegistrationRepository registrations = provider.getIfAvailable();
        http.authorizeHttpRequests(rules -> rules
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/assets/**").permitAll()
                .requestMatchers("/account", "/api/me").authenticated()
                .requestMatchers("/app", "/areas/**", "/tasks/**", "/employees/**", "/task-groups/**",
                        "/teams/**", "/payroll/**", "/api/payrolls/**")
                .access((authentication, context) -> {
                    if (!(authentication.get() instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken)) {
                        return new org.springframework.security.authorization.AuthorizationDecision(false);
                    }
                    try {
                        accounts.requireCurrent();
                        return new org.springframework.security.authorization.AuthorizationDecision(true);
                    } catch (org.springframework.security.core.AuthenticationException
                            | org.springframework.security.access.AccessDeniedException denied) {
                        return new org.springframework.security.authorization.AuthorizationDecision(false);
                    }
                })
                .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(registrations == null
                        ? new HttpStatusEntryPoint(HttpStatus.SERVICE_UNAVAILABLE)
                        : new LoginUrlAuthenticationEntryPoint("/")))
                .logout(logout -> logout.logoutSuccessUrl("/"));
        if (registrations != null) {
            var logout = new OidcClientInitiatedLogoutSuccessHandler(registrations);
            logout.setPostLogoutRedirectUri(logoutRedirect);
            var authorizationRequests = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
            if (!publicOrigin.isEmpty()) {
                // Persist the same canonical redirect in the authorization request
                // that will be used for the code exchange; ignore forwarded hosts.
                authorizationRequests.setAuthorizationRequestCustomizer(request ->
                        request.redirectUri(logoutRedirect + "login/oauth2/code/auth0"));
            }
            http.oauth2Login(login -> login.loginPage("/").defaultSuccessUrl("/account", true)
                    .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorizationRequests))
                    .failureUrl("/?loginError"))
                    .logout(config -> config.logoutSuccessHandler(logout));
        }
        // CSRF, session fixation protection and default response headers remain enabled.
        return http.build();
    }

    static String postLogoutRedirect(String publicOrigin, boolean production) {
        if (publicOrigin.isEmpty()) {
            if (production) throw new IllegalArgumentException("APP_PUBLIC_URL is required in production");
            return "{baseUrl}/";
        }
        var origin = java.net.URI.create(publicOrigin);
        if (!"https".equals(origin.getScheme()) || origin.getHost() == null || origin.getUserInfo() != null
                || origin.getRawQuery() != null || origin.getRawFragment() != null
                || (origin.getRawPath() != null && !origin.getRawPath().isEmpty())
                || (origin.getPort() != -1 && origin.getPort() != 443)) {
            throw new IllegalArgumentException("APP_PUBLIC_URL must be an HTTPS origin without a trailing slash");
        }
        return origin.toASCIIString() + "/";
    }

    @Bean
    UserDetailsService noLocalPasswords() {
        return username -> { throw new UsernameNotFoundException("Use the configured OIDC provider"); };
    }
}
