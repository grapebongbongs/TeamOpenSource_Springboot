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
        if (userService.existsByUsername(username)) {
            model.addAttribute("error", "이미 존재하는 아이디입니다.");
            return "user/signup_form";
        }
        userService.createUser(username, password, email);
        model.addAttribute("msg", "회원가입 완료!");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "user/login_form";
    }
}

