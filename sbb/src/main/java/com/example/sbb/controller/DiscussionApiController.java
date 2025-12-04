package com.example.sbb.controller;

import com.example.sbb.domain.quiz.QuestionDiscussion;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.service.DiscussionService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionApiController {

    private final DiscussionService discussionService;
    private final UserService userService;

    @GetMapping("/{questionId}")
    public ResponseEntity<?> list(@PathVariable Long questionId, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SiteUser user = userService.getUser(principal.getName());
        QuizQuestion question = discussionService.getOwnedQuestion(user, questionId);
        if (question == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no question");

        List<QuestionDiscussion> items = discussionService.list(question);
        return ResponseEntity.ok(items.stream().map(DiscussionPayload::from).toList());
    }

    @PostMapping("/{questionId}")
    public ResponseEntity<?> add(@PathVariable Long questionId,
                                 @RequestParam("content") String content,
                                 Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SiteUser user = userService.getUser(principal.getName());
        QuizQuestion question = discussionService.getOwnedQuestion(user, questionId);
        if (question == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("no question");

        QuestionDiscussion saved = discussionService.addComment(question, user, content);
        if (saved == null) return ResponseEntity.badRequest().body("empty");
        return ResponseEntity.ok(DiscussionPayload.from(saved));
    }

    @Data
    @AllArgsConstructor
    static class DiscussionPayload {
        private Long id;
        private String username;
        private String content;
        private LocalDateTime createdAt;

        static DiscussionPayload from(QuestionDiscussion qd) {
            return new DiscussionPayload(
                    qd.getId(),
                    qd.getUser().getUsername(),
                    qd.getContent(),
                    qd.getCreatedAt()
            );
        }
    }
}
