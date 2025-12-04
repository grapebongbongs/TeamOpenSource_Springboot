package com.example.sbb.controller;

import com.example.sbb.domain.user.Friend;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.FriendService;
import com.example.sbb.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final UserService userService;
    private final FriendService friendService;
    private final ProgressService progressService;

    private record RankingEntry(int rank, SiteUser user, String medal) {}

    @GetMapping("/")
    public String index(Model model, java.security.Principal principal) {
        if (principal != null) {
            SiteUser user = userService.getUser(principal.getName());
            model.addAttribute("sessionUser", user);
            var stats = progressService.computeStats(user);
            model.addAttribute("todaySolvedCount", stats.todaySolved());

            List<SiteUser> pool = new ArrayList<>();
            pool.add(user);
            List<Friend> friends = friendService.myFriends(user);
            for (Friend f : friends) {
                if (f.getTo() != null) pool.add(f.getTo());
            }

            pool.sort(Comparator.comparingInt((SiteUser u) -> u.getPoints()).reversed()
                    .thenComparing(SiteUser::getUsername));
            List<RankingEntry> ranking = new ArrayList<>();
            int rank = 1;
            for (SiteUser u : pool) {
                if (ranking.size() >= 5) break;
                String medal = switch (rank) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "";
                };
                ranking.add(new RankingEntry(rank, u, medal));
                rank++;
            }
            model.addAttribute("weeklyRanking", ranking);
        }
        return "index";
    }
}
