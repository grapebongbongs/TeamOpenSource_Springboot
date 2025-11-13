package com.example.sbb.repository;

import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    // 아직 안 푼 문제
    List<QuizQuestion> findByUserAndSolvedFalseOrderByCreatedAtAsc(SiteUser user);

    // 맞은 문제
    List<QuizQuestion> findByUserAndSolvedTrueAndCorrectTrueOrderByCreatedAtAsc(SiteUser user);

    // 틀린 문제
    List<QuizQuestion> findByUserAndSolvedTrueAndCorrectFalseOrderByCreatedAtAsc(SiteUser user);
}
