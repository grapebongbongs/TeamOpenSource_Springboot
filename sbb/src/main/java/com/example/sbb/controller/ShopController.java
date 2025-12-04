package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
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
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final UserService userService;
    private final ProgressService progressService;

    private record AvatarOption(String value, String label, int cost) {}
    private record BannerOption(String value, String label, int cost, String gradient) {}

    @GetMapping("/shop")
    public String shop(Model model, Principal principal, RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        var stats = progressService.computeStats(user);
        var badges = progressService.badges(user, stats);

        Set<String> ownedAvatars = userService.parseOwned(user.getPurchasedAvatars());
        Set<String> ownedBanners = userService.parseOwned(user.getPurchasedBanners());
        Set<String> ownedBadges = userService.parseOwned(user.getPurchasedBadges());

        Comparator<Object> ownedFirst = (a, b) -> {
            boolean aOwned = isOwned(a, ownedAvatars, ownedBanners, ownedBadges);
            boolean bOwned = isOwned(b, ownedAvatars, ownedBanners, ownedBadges);
            return Boolean.compare(!aOwned, !bOwned); // owned true -> comes first
        };

        List<AvatarOption> avatars = List.of(
                new AvatarOption("🧑", "기본", 0),
                new AvatarOption("🐱", "냥냥이", 10),
                new AvatarOption("🐳", "고래", 10),
                new AvatarOption("🦊", "여우", 10),
                new AvatarOption("🐯", "호랑이", 12),
                new AvatarOption("🐼", "판다", 12),
                new AvatarOption("👾", "스페이스몬", 14),
                new AvatarOption("🤖", "로봇", 14),
                new AvatarOption("🐉", "드래곤", 16),
                new AvatarOption("🛰️", "위성", 16)
        ).stream().sorted((a,b)->{
            boolean ao = ownedAvatars.contains(a.value());
            boolean bo = ownedAvatars.contains(b.value());
            return Boolean.compare(!ao, !bo);
        }).toList();

        List<BannerOption> banners = List.of(
                new BannerOption("sunrise", "Sunrise", 10, "linear-gradient(120deg,#fde68a,#f97316)"),
                new BannerOption("ocean", "Ocean", 15, "linear-gradient(120deg,#38bdf8,#6366f1)"),
                new BannerOption("forest", "Forest", 15, "linear-gradient(120deg,#22c55e,#0f766e)"),
                new BannerOption("midnight", "Midnight", 18, "linear-gradient(120deg,#111827,#334155)"),
                new BannerOption("aurora", "Aurora", 20, "linear-gradient(120deg,#6366f1,#06b6d4,#22d3ee)")
        ).stream().sorted((a,b)->{
            boolean ao = ownedBanners.contains(a.value());
            boolean bo = ownedBanners.contains(b.value());
            return Boolean.compare(!ao, !bo);
        }).toList();

        model.addAttribute("avatarOptions", avatars);
        model.addAttribute("bannerOptions", banners);
        model.addAttribute("ownedAvatars", ownedAvatars);
        model.addAttribute("ownedBanners", ownedBanners);
        model.addAttribute("badges", badges);
        model.addAttribute("ownedBadges", ownedBadges);
        model.addAttribute("boostPrices", userService.getBoostPrices());
        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        return "shop";
    }

    @PostMapping("/shop/avatar")
    public String buyAvatar(@RequestParam("avatar") String avatar,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = userService.updateAvatar(user, avatar);
        rttr.addFlashAttribute(ok ? "message" : "error",
                ok ? "아바타를 적용했습니다." : "포인트가 부족하거나 구매할 수 없습니다.");
        return "redirect:/shop";
    }

    @PostMapping("/shop/banner")
    public String buyBanner(@RequestParam("banner") String banner,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = userService.updateBanner(user, banner);
        rttr.addFlashAttribute(ok ? "message" : "error",
                ok ? "배너를 적용했습니다." : "포인트가 부족하거나 구매할 수 없습니다.");
        return "redirect:/shop";
    }

    @PostMapping("/shop/badge")
    public String equipBadge(@RequestParam("badgeId") String badgeId,
                             Principal principal,
                             RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        var stats = progressService.computeStats(user);
        var badgeStatuses = progressService.badges(user, stats);
        Set<String> unlocked = badgeStatuses.stream()
                .filter(b -> b.unlocked())
                .map(ProgressService.BadgeStatus::id)
                .collect(java.util.stream.Collectors.toSet());
        boolean ok = userService.equipBadge(user, badgeId, unlocked);
        rttr.addFlashAttribute(ok ? "message" : "error",
                ok ? "뱃지를 장착했습니다." : "해금되지 않은 뱃지입니다.");
        return "redirect:/shop";
    }

    @PostMapping("/shop/boost")
    public String buyBoost(@RequestParam("boostId") String boostId,
                           Principal principal,
                           RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = userService.purchaseBoost(user, boostId);
        String msg = switch (boostId) {
            case "shield" -> "연속 보호 아이템을 구입했습니다.";
            case "extra10" -> "추가 문제 생성 토큰 10개를 구입했습니다.";
            case "extra20" -> "추가 문제 생성 토큰 20개를 구입했습니다.";
            default -> "아이템을 구입했습니다.";
        };
        rttr.addFlashAttribute(ok ? "message" : "error",
                ok ? msg : "포인트가 부족하거나 구입할 수 없습니다.");
        return "redirect:/shop";
    }

    private boolean isOwned(Object obj, Set<String> avatars, Set<String> banners, Set<String> badges) {
        if (obj instanceof AvatarOption ao) return avatars.contains(ao.value());
        if (obj instanceof BannerOption bo) return banners.contains(bo.value());
        return false;
    }
}
