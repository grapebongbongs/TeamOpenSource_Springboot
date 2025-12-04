package com.example.sbb.service;

import com.example.sbb.domain.quiz.QuestionDiscussion;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.QuestionDiscussionRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final QuestionDiscussionRepository discussionRepository;
    private final QuizQuestionRepository quizQuestionRepository;

    @Transactional(readOnly = true)
    public QuizQuestion getOwnedQuestion(SiteUser user, Long questionId) {
        if (user == null || questionId == null) return null;
        QuizQuestion question = quizQuestionRepository.findById(questionId).orElse(null);
        if (question == null) return null;
        if (!question.getUser().getId().equals(user.getId())) return null;
        return question;
    }

    @Transactional(readOnly = true)
    public QuizQuestion getQuestion(Long questionId) {
        if (questionId == null) return null;
        return quizQuestionRepository.findById(questionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<QuestionDiscussion> list(QuizQuestion question) {
        if (question == null) return Collections.emptyList();
        return discussionRepository.findByQuestionOrderByCreatedAtAsc(question);
    }

    @Transactional
    public QuestionDiscussion addComment(QuizQuestion question, SiteUser user, String content) {
        if (question == null || user == null || content == null || content.isBlank()) return null;
        QuestionDiscussion discussion = new QuestionDiscussion();
        discussion.setQuestion(question);
        discussion.setUser(user);
        discussion.setContent(content.trim());
        return discussionRepository.save(discussion);
    }
}
