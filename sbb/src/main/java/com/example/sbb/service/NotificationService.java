package com.example.sbb.service;

import com.example.sbb.domain.notification.PendingNotification;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.PendingNotificationRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PendingNotificationRepository pendingNotificationRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void scheduleTwiceDaily() {
        LocalDate today = LocalDate.now();
        LocalDateTime[] slots = new LocalDateTime[] {
                LocalDateTime.of(today, LocalTime.of(9, 0)),
                LocalDateTime.of(today, LocalTime.of(21, 0))
        };

        userRepository.findAll().forEach(user -> {
            List<QuizQuestion> candidates =
                    quizQuestionRepository.findByUserAndSolvedFalseOrderByCreatedAtAsc(user);

            for (LocalDateTime slot : slots) {
                scheduleForSlot(user, candidates, slot, 1);
            }
        });
    }

    @Transactional
    public void scheduleForSlot(SiteUser user, List<QuizQuestion> pool, LocalDateTime dueAt, int count) {
        if (pool == null || pool.isEmpty()) return;
        int scheduled = 0;
        for (QuizQuestion q : pool) {
            if (scheduled >= count) break;
            boolean exists = pendingNotificationRepository
                    .existsByUser_IdAndQuestion_IdAndDueAt(user.getId(), q.getId(), dueAt);
            if (exists) continue;
            PendingNotification pn = new PendingNotification();
            pn.setUser(user);
            pn.setQuestion(q);
            pn.setDueAt(dueAt);
            pendingNotificationRepository.save(pn);
            scheduled++;
        }
    }

    @Transactional
    public List<PendingNotification> consumeDue(SiteUser user) {
        List<PendingNotification> due = pendingNotificationRepository
                .findByUserAndDeliveredFalseAndDueAtBefore(user, LocalDateTime.now().plusMinutes(1));
        List<PendingNotification> delivered = new ArrayList<>();
        for (PendingNotification pn : due) {
            pn.setDelivered(true);
            delivered.add(pn);
        }
        pendingNotificationRepository.saveAll(delivered);
        return delivered;
    }
}
