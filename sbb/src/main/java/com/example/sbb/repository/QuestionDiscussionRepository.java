package com.example.sbb.repository;

import com.example.sbb.domain.quiz.QuestionDiscussion;
import com.example.sbb.domain.quiz.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionDiscussionRepository extends JpaRepository<QuestionDiscussion, Long> {
    List<QuestionDiscussion> findByQuestionOrderByCreatedAtAsc(QuizQuestion question);
}
