package com.example.sbb.domain.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SiteUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    private String role;  // ROLE_USER, ROLE_ADMIN 등

    @Column(nullable = false, columnDefinition = "int default 0")
    private int points = 0;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int streak = 0;

    private java.time.LocalDate lastSolvedDate;

    private String avatar; // 예: 🧑, 🐱 등

    private String banner; // 예: sunrise, ocean, forest

    // 콤마(,)로 구분된 보유 아바타/배너 목록
    @Column(columnDefinition = "TEXT")
    private String purchasedAvatars;

    @Column(columnDefinition = "TEXT")
    private String purchasedBanners;

    @Column(columnDefinition = "TEXT")
    private String purchasedBadges;

    private String activeBadge;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int shieldItems = 0; // 연속일 보호 아이템

    @Column(nullable = false, columnDefinition = "int default 0")
    private int extraProblemTokens = 0; // 추가 문제 생성 토큰

    @Column(nullable = false, columnDefinition = "int default 60")
    private int dailyGoalMinutes = 60;
}
