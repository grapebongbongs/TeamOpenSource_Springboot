package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class StatsController {

    private final UserService userService;
    private final ProgressService progressService;

    @GetMapping("/stats")
    public String stats(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        var stats = progressService.computeStats(user);
        var badges = progressService.badges(user, stats);
        int level = Math.max(1, (user.getPoints() / 50) + 1);
        String tier = tier(level);
        String activeBadgeName = badges.stream()
                .filter(b -> b.equipped())
                .findFirst()
                .map(ProgressService.BadgeStatus::name)
                .orElseGet(() -> user.getActiveBadge() != null ? user.getActiveBadge() : "자라나는 새싹");

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("level", level);
        model.addAttribute("tier", tier);
        model.addAttribute("badges", badges);
        model.addAttribute("activeBadgeName", activeBadgeName);
        model.addAttribute("xpGraph", buildXpGraph(stats.totalSolved()));
        model.addAttribute("weakness", stats.weakest().orElse(null));
        return "stats";
    }

    private int[] buildXpGraph(long solved) {
        int current = (int) Math.min(100, solved);
        int target = Math.min(100, (int)((solved % 50) * 2));
        return new int[]{current, target};
    }

    private String tier(int level) {
        if (level >= 20) return "Legend";
        if (level >= 15) return "Master";
        if (level >= 10) return "Expert";
        if (level >= 5) return "Advanced";
        return "Rookie";
    }
}
