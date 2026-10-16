package com.taskapp.task_scheduler.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:no_auth_provider;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.jpa.show-sql=false", "spring.sql.init.mode=never"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class UnconfiguredLoginTests {
    @Autowired MockMvc http;

    @Test
    void missingProviderNeverOpensBusinessRoutesOrOffersABrokenLogin() throws Exception {
        http.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("temporalmente fuera de servicio")))
                .andExpect(content().string(not(containsString("href=\"/oauth2/authorization/auth0\""))));
        for (String path : new String[]{"/app", "/areas", "/api/me", "/h2-console/", "/oauth2/authorization/auth0"}) {
            http.perform(get(path)).andExpect(status().isServiceUnavailable());
        }
        http.perform(get("/areas").with(user("anyone"))).andExpect(status().isForbidden());
        http.perform(post("/areas/guardar").with(csrf())).andExpect(status().isServiceUnavailable());
    }
}
