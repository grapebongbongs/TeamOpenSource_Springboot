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
}
