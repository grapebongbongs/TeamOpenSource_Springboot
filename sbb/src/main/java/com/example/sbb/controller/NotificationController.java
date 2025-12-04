package com.example.sbb.controller;

import com.example.sbb.domain.notification.PendingNotification;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/due")
    public ResponseEntity<?> due(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        SiteUser user = userService.getUser(principal.getName());
        List<PendingNotification> due = notificationService.consumeDue(user);
        List<NotificationPayload> payloads = due.stream()
                .map(NotificationPayload::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payloads);
    }

    @GetMapping("/schedule-now")
    public ResponseEntity<?> scheduleNow(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        notificationService.scheduleTwiceDaily();
        return ResponseEntity.ok().build();
    }

    @Data
    @AllArgsConstructor
    static class NotificationPayload {
        private Long id;
        private String title;
        private String body;
        private String url;

        static NotificationPayload from(PendingNotification pn) {
            String title = "새 문제 도착";
            String body = pn.getQuestion().getQuestionText();
            String url = "/quiz/solve/" + pn.getQuestion().getId();
            return new NotificationPayload(pn.getId(), title, body, url);
        }
    }
}
