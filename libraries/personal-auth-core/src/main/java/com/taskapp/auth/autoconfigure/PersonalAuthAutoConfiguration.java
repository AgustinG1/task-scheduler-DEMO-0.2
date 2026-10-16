package com.taskapp.auth.autoconfigure;

import com.taskapp.auth.CurrentUser;
import com.taskapp.auth.SecurityContextCurrentUser;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/** Registers identity access only. HTTP security and business authorization belong to each app. */
@AutoConfiguration
@ConditionalOnClass(OidcUser.class)
public class PersonalAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CurrentUser.class)
    SecurityContextCurrentUser currentUser() {
        return new SecurityContextCurrentUser();
    }
}
