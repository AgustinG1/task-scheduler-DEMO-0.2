package com.taskapp.task_scheduler.controller;

import com.taskapp.auth.CurrentUser;
import com.taskapp.task_scheduler.security.AppUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AccountController {
    private final CurrentUser currentUser;
    private final AppUserService users;
    private final ObjectProvider<ClientRegistrationRepository> registrations;

    public AccountController(CurrentUser currentUser, AppUserService users,
            ObjectProvider<ClientRegistrationRepository> registrations) {
        this.currentUser = currentUser;
        this.users = users;
        this.registrations = registrations;
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("signedIn", currentUser.find().isPresent());
        var clients = registrations.getIfAvailable();
        model.addAttribute("loginAvailable", clients != null && clients.findByRegistrationId("auth0") != null);
        return "account/index";
    }

    @GetMapping("/account")
    String account(Model model, HttpServletResponse response) {
        if (!currentUser.require().hasVerifiedEmail()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return "account/verify-email";
        }
        model.addAttribute("profile", users.requireCurrent());
        return "account/profile";
    }

    @GetMapping("/api/me")
    @ResponseBody
    AppUserService.Account ownAccount() {
        return users.requireCurrent();
    }
}
