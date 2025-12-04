package com.example.sbb.controller;

import com.example.sbb.domain.group.GroupInvite;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.GroupService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/groups/invite")
@RequiredArgsConstructor
public class GroupInviteApiController {

    private final GroupService groupService;
    private final UserService userService;

    @GetMapping("/inbox")
    public ResponseEntity<?> inbox(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        List<GroupInvite> recv = groupService.inboxInvites(me);
        List<GroupInvite> sent = groupService.sentInvites(me);
        return ResponseEntity.ok(
                java.util.Map.of(
                        "received", recv.stream().map(GroupInvitePayload::from).toList(),
                        "sent", sent.stream().map(GroupInvitePayload::from).toList()
                )
        );
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = groupService.acceptInvite(id, me);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = groupService.rejectInvite(id, me);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok().build();
    }

    @Data
    @AllArgsConstructor
    static class GroupInvitePayload {
        private Long id;
        private String groupName;
        private String from;
        private String to;
        private String code;
        private LocalDateTime createdAt;

        static GroupInvitePayload from(GroupInvite inv) {
            return new GroupInvitePayload(
                    inv.getId(),
                    inv.getGroup() != null ? inv.getGroup().getName() : "",
                    inv.getFromUser() != null ? inv.getFromUser().getUsername() : "",
                    inv.getToUser() != null ? inv.getToUser().getUsername() : "",
                    inv.getGroup() != null ? inv.getGroup().getJoinCode() : "",
                    inv.getCreatedAt()
            );
        }
    }
}
