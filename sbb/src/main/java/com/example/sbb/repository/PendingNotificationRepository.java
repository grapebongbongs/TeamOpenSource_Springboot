package com.example.sbb.repository;

import com.example.sbb.domain.notification.PendingNotification;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingNotificationRepository extends JpaRepository<PendingNotification, Long> {
    List<PendingNotification> findByUserAndDeliveredFalseAndDueAtBefore(SiteUser user, LocalDateTime now);
    boolean existsByUser_IdAndQuestion_IdAndDueAt(Long userId, Long questionId, LocalDateTime dueAt);
}
