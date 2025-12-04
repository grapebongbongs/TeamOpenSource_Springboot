package com.example.sbb.controller;

import com.example.sbb.domain.user.Friend;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final FriendService friendService;

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<Friend> friends = friendService.myFriends(user);
        model.addAttribute("user", user);
        model.addAttribute("friends", friends);
        return "profile";
    }

    @PostMapping("/friends/request")
    public String requestFriend(@RequestParam("username") String username,
                                Principal principal,
                                Model model) {
        if (principal == null) return "redirect:/login";
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.sendRequest(me, username);
        if (!ok) {
            model.addAttribute("error", "친구 요청을 보낼 수 없습니다. 아이디를 확인하세요.");
        } else {
            model.addAttribute("message", "친구 요청을 보냈습니다.");
        }
        return profile(model, principal);
    }

    @PostMapping("/friends/remove")
    public String removeFriend(@RequestParam("friendId") Long friendId,
                               Principal principal,
                               Model model) {
        if (principal == null) return "redirect:/login";
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.removeFriend(me, friendId);
        if (!ok) {
            model.addAttribute("error", "친구를 삭제할 수 없습니다.");
        } else {
            model.addAttribute("message", "친구를 삭제했습니다.");
        }
        return profile(model, principal);
    }
}
