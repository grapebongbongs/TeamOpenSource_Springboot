package com.example.sbb.controller;

import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/signup")
    public String signupForm() {
        return "user/signup_form";
    }

    @PostMapping("/signup")
    public String signupSubmit(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String email,
                               Model model) {
        userService.createUser(username, password, email);
        model.addAttribute("msg", "회원가입 완료!");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "user/login_form";
    }
}
