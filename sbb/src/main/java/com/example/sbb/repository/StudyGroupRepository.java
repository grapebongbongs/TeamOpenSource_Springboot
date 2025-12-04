package com.example.sbb.repository;

import com.example.sbb.domain.group.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    Optional<StudyGroup> findByJoinCode(String joinCode);
}
