package com.example.sbb.repository;

import com.example.sbb.domain.StudySession;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUserAndDateBetween(SiteUser user, LocalDate start, LocalDate end);
    List<StudySession> findByUserAndDate(SiteUser user, LocalDate date);
}
