package com.example.sbb.controller;

import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.FriendRequest;
import com.example.sbb.service.FriendService;
import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendApiController {
    private final FriendService friendService;
    private final UserService userService;

    @GetMapping("/inbox")
    @ResponseBody
    public ResponseEntity<?> inbox(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        List<FriendRequest> pending = friendService.pendingInbox(me);
        List<FriendRequest> sentPending = friendService.pendingSent(me);
        List<FriendRequest> accepted = friendService.acceptedSent(me);
        var shareReceived = friendService.pendingShareInbox(me);
        var shareSent = friendService.sentSharePending(me);
        var shareAccepted = friendService.sentShareAccepted(me);
        return ResponseEntity.ok(Map.of(
                "received", pending.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "from", r.getFromUser().getUsername(),
                        "createdAt", r.getCreatedAt()
                )).toList(),
                "sent", sentPending.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "to", r.getToUser().getUsername(),
                        "createdAt", r.getCreatedAt()
                )).toList(),
                "accepted", accepted.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "to", r.getToUser().getUsername(),
                        "createdAt", r.getCreatedAt()
                )).toList(),
                "shareReceived", shareReceived.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "from", r.getFromUser().getUsername(),
                        "questionId", r.getQuestion().getId(),
                        "questionText", r.getQuestion().getQuestionText(),
                        "createdAt", r.getCreatedAt()
                )).toList(),
                "shareSent", shareSent.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "to", r.getToUser().getUsername(),
                        "questionId", r.getQuestion().getId(),
                        "questionText", r.getQuestion().getQuestionText(),
                        "createdAt", r.getCreatedAt()
                )).toList(),
                "shareAccepted", shareAccepted.stream().map(r -> Map.of(
                        "id", r.getId(),
                        "to", r.getToUser().getUsername(),
                        "questionId", r.getQuestion().getId(),
                        "questionText", r.getQuestion().getQuestionText(),
                        "createdAt", r.getCreatedAt()
                )).toList()
        ));
    }

    @PostMapping("/request")
    @ResponseBody
    public ResponseEntity<?> request(@RequestParam("username") String username,
                                     Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.sendRequest(me, username);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/request/{id}/accept")
    @ResponseBody
    public ResponseEntity<?> accept(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.acceptRequest(id, me);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/request/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> reject(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.rejectRequest(id, me);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/share")
    @ResponseBody
    public ResponseEntity<?> shareQuestion(@RequestParam("friendId") Long friendId,
                                           @RequestParam("questionId") Long questionId,
                                           Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.shareQuestionToFriend(me, friendId, questionId);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/share/bulk")
    @ResponseBody
    public ResponseEntity<?> shareQuestionBulk(@RequestParam("friendIds") List<Long> friendIds,
                                               @RequestParam("questionId") Long questionId,
                                               Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.sendShareRequests(me, friendIds, questionId);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/share/{id}/accept")
    @ResponseBody
    public ResponseEntity<?> acceptShare(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.acceptShareRequest(id, me);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }

    @PostMapping("/share/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectShare(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser me = userService.getUser(principal.getName());
        boolean ok = friendService.rejectShareRequest(id, me);
        if (!ok) return ResponseEntity.badRequest().body("failed");
        return ResponseEntity.ok(Map.of("status","ok"));
    }
}
