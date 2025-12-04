package com.example.sbb.controller;

import com.example.sbb.domain.user.Friend;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.FriendShareRequestRepository;
import com.example.sbb.service.GroupService;
import com.example.sbb.service.FriendService;
import com.example.sbb.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final FriendService friendService;
    private final GroupService groupService;
    private final ProgressService progressService;
    private final FriendShareRequestRepository friendShareRequestRepository;

    private record AvatarOption(String value, String label, int cost) {}
    private record BannerOption(String value, String label, int cost, String gradient) {}

    @GetMapping("/profile")
    public String profile(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<Friend> friends = friendService.myFriends(user);
        model.addAttribute("user", user);
        model.addAttribute("friends", friends);
        model.addAttribute("ownedAvatars", userService.parseOwned(user.getPurchasedAvatars()));
        model.addAttribute("ownedBanners", userService.parseOwned(user.getPurchasedBanners()));
        model.addAttribute("ownedBadges", userService.parseOwned(user.getPurchasedBadges()));
        model.addAttribute("titleOptions", buildTitleOptions(user));
        model.addAttribute("memberships", groupService.memberships(user));
        model.addAttribute("shareInbox", friendShareRequestRepository.findByToUserAndStatus(user, com.example.sbb.domain.user.FriendShareRequest.Status.PENDING));
        model.addAttribute("shareAccepted", friendShareRequestRepository.findByToUserAndStatus(user, com.example.sbb.domain.user.FriendShareRequest.Status.ACCEPTED));
        model.addAttribute("progressStats", progressService.computeStats(user));
        return "profile";
    }

    @GetMapping("/profile/customize")
    public String customize(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        model.addAttribute("avatarOptions", List.of(
                new AvatarOption("🧑", "기본", 0),
                new AvatarOption("🐱", "냥냥이", 10),
                new AvatarOption("🐳", "고래", 10),
                new AvatarOption("🦊", "여우", 10),
                new AvatarOption("🐯", "호랑이", 12),
                new AvatarOption("🐼", "판다", 12),
                new AvatarOption("👾", "스페이스몬", 14),
                new AvatarOption("🤖", "로봇", 14)
        ));
        model.addAttribute("bannerOptions", List.of(
                new BannerOption("sunrise", "Sunrise", 10, "linear-gradient(90deg,#f59e0b,#f97316)"),
                new BannerOption("ocean", "Ocean", 15, "linear-gradient(90deg,#06b6d4,#3b82f6)"),
                new BannerOption("forest", "Forest", 15, "linear-gradient(90deg,#10b981,#065f46)"),
                new BannerOption("midnight", "Midnight", 18, "linear-gradient(90deg,#0f172a,#1e293b)"),
                new BannerOption("aurora", "Aurora", 20, "linear-gradient(90deg,#6366f1,#06b6d4,#22d3ee)")
        ));
        Set<String> ownedAvatars = userService.parseOwned(user.getPurchasedAvatars());
        if (user.getAvatar() != null) ownedAvatars.add(user.getAvatar());
        Set<String> ownedBanners = userService.parseOwned(user.getPurchasedBanners());
        if (user.getBanner() != null) ownedBanners.add(user.getBanner());
        model.addAttribute("ownedAvatars", ownedAvatars);
        model.addAttribute("ownedBanners", ownedBanners);
        model.addAttribute("user", user);
        return "profile_customize";
    }

    @PostMapping("/profile/customize/avatar")
    public String setAvatar(@RequestParam("avatar") String avatar,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        Set<String> owned = userService.parseOwned(user.getPurchasedAvatars());
        boolean hadBefore = owned.contains(avatar);
        boolean ok = userService.updateAvatar(user, avatar);
        if (!ok) rttr.addFlashAttribute("error", "포인트가 부족하거나 저장할 수 없습니다.");
        else if (hadBefore) rttr.addFlashAttribute("message", "아바타를 변경했습니다.");
        else rttr.addFlashAttribute("message", "아바타를 구매하고 장착했습니다.");
        return "redirect:/profile";
    }

    @PostMapping("/profile/customize/banner")
    public String setBanner(@RequestParam("banner") String banner,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        Set<String> owned = userService.parseOwned(user.getPurchasedBanners());
        boolean hadBefore = owned.contains(banner);
        boolean ok = userService.updateBanner(user, banner);
        if (!ok) rttr.addFlashAttribute("error", "포인트가 부족하거나 저장할 수 없습니다.");
        else if (hadBefore) rttr.addFlashAttribute("message", "배너를 변경했습니다.");
        else rttr.addFlashAttribute("message", "배너를 구매하고 장착했습니다.");
        return "redirect:/profile";
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

    private List<String> buildTitleOptions(SiteUser user) {
        Set<String> ownedBadges = userService.parseOwned(user.getPurchasedBadges());
        List<String> titles = new java.util.ArrayList<>();
        titles.add("자라나는 새싹");
        titles.addAll(ownedBadges);
        return titles;
    }
}
