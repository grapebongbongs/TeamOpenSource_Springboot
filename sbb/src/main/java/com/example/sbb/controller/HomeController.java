package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

@Controller
@RequiredArgsConstructor
public class HomeController {
  private final UserService userService;
  @GetMapping("/")
  public String index(Model model, java.security.Principal principal) {
    if (principal != null) {
      SiteUser user = userService.getUser(principal.getName());
      model.addAttribute("sessionUser", user);
    }
    return "index";
  }
}
