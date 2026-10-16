package com.taskapp.authdemo;

import com.taskapp.auth.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ProfileController {
    private final CurrentUser currentUser;

    public ProfileController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/")
    String index(Model model) {
        model.addAttribute("signedIn", currentUser.find().isPresent());
        return "index";
    }

    @GetMapping("/profile")
    String profile(Model model) {
        model.addAttribute("profile", ownProfile());
        return "profile";
    }

    @GetMapping("/api/me")
    @ResponseBody
    ProfileView ownProfile() {
        var identity = currentUser.requireVerifiedEmail();
        return new ProfileView(identity.displayName(), identity.email());
    }

    public record ProfileView(String displayName, String email) {}
}
