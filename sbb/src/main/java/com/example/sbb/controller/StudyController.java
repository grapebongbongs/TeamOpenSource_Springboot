package com.example.sbb.controller;

import com.example.sbb.domain.StudySession;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/study")
public class StudyController {
    private final UserService userService;
    private final StudySessionRepository studySessionRepository;

    @PostMapping("/log")
    @ResponseBody
    public ResponseEntity<?> log(@RequestParam("seconds") int seconds, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (seconds <= 0) return ResponseEntity.badRequest().body("seconds > 0");
        SiteUser user = userService.getUser(principal.getName());
        StudySession s = new StudySession();
        s.setUser(user);
        s.setDate(LocalDate.now());
        s.setSeconds(seconds);
        studySessionRepository.save(s);
        long totalToday = studySessionRepository.findByUserAndDate(user, LocalDate.now()).stream()
                .mapToLong(StudySession::getSeconds).sum();
        return ResponseEntity.ok(Map.of("todaySeconds", totalToday));
    }

    @PostMapping("/goal")
    @ResponseBody
    public ResponseEntity<?> setGoal(@RequestParam("minutes") int minutes, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (minutes < 0) return ResponseEntity.badRequest().body("minutes >= 0");
        SiteUser user = userService.getUser(principal.getName());
        user.setDailyGoalMinutes(minutes);
        userService.save(user);
        return ResponseEntity.ok(Map.of("goal", minutes));
    }
}
