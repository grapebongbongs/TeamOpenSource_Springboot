package com.example.sbb.controller;

import com.example.sbb.domain.user.FriendShareRequest;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.FriendShareRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SharedController {
    private final UserService userService;
    private final FriendShareRequestRepository friendShareRequestRepository;

    @GetMapping("/shared")
    public String shared(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<FriendShareRequest> inbox = friendShareRequestRepository.findByToUserAndStatus(user, FriendShareRequest.Status.PENDING);
        List<FriendShareRequest> accepted = friendShareRequestRepository.findByToUserAndStatus(user, FriendShareRequest.Status.ACCEPTED);
        model.addAttribute("inbox", inbox);
        model.addAttribute("accepted", accepted);
        return "shared";
    }
}
