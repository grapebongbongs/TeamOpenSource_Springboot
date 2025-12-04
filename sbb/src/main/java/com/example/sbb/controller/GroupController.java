package com.example.sbb.controller;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.GroupService;
import com.example.sbb.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;
    private final FriendService friendService;

    @GetMapping
    public String list(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);
        model.addAttribute("memberships", memberships);
        return "group_list";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         Principal principal,
                         RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        StudyGroup group = groupService.createGroup(name, user);
        rttr.addFlashAttribute("message", "그룹이 생성되었습니다. 코드: " + group.getJoinCode());
        return "redirect:/groups";
    }

    @PostMapping("/join")
    public String join(@RequestParam("code") String code,
                       Principal principal,
                       RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        Optional<StudyGroup> group = groupService.joinGroupByCode(code.trim(), user);
        if (group.isPresent()) {
            rttr.addFlashAttribute("message", "그룹에 참여했습니다: " + group.get().getName());
        } else {
            rttr.addFlashAttribute("error", "그룹 코드를 찾을 수 없습니다.");
        }
        return "redirect:/groups";
    }

    @PostMapping("/{groupId}/share/{questionId}")
    public String share(@PathVariable Long groupId,
                        @PathVariable Long questionId,
                        Principal principal,
                        RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        boolean ok = groupService.shareQuestion(groupId, questionId, user);
        if (ok) {
            rttr.addFlashAttribute("message", "그룹에 문제를 공유했습니다.");
        } else {
            rttr.addFlashAttribute("error", "공유할 수 없습니다. 그룹 멤버인지 또는 자신의 문제인지 확인하세요.");
        }
        return "redirect:/groups/" + groupId;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         Principal principal,
                         Model model,
                         RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<GroupMember> memberships = groupService.memberships(user);
        Optional<StudyGroup> groupOpt = memberships.stream()
                .map(GroupMember::getGroup)
                .filter(g -> g.getId().equals(id))
                .findFirst();
        if (groupOpt.isEmpty()) {
            rttr.addFlashAttribute("error", "그룹에 속해 있지 않습니다.");
            return "redirect:/groups";
        }
        StudyGroup group = groupOpt.get();
        model.addAttribute("group", group);
        model.addAttribute("sharedQuestions", groupService.listShared(group));
        model.addAttribute("memberships", memberships);
        model.addAttribute("friends", friendService.myFriends(user));
        return "group_list";
    }

    @PostMapping("/use-shared/{sharedId}")
    public String useShared(@PathVariable Long sharedId,
                            Principal principal,
                            RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        var copy = groupService.cloneSharedForUser(sharedId, user);
        if (copy == null) {
            rttr.addFlashAttribute("error", "공유된 문제를 사용할 수 없습니다.");
            return "redirect:/groups";
        }
        rttr.addFlashAttribute("message", "문제를 내 문제에 추가했습니다.");
        return "redirect:/quiz/list?folderId=" + (copy.getFolder() != null ? copy.getFolder().getId() : "");
    }
}
