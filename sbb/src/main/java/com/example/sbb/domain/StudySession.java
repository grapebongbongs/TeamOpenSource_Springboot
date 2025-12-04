package com.example.sbb.domain;

import com.example.sbb.domain.user.SiteUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class StudySession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private SiteUser user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int seconds;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
