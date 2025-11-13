package com.example.sbb.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class QuestionCache {

    private final List<String> questions = new ArrayList<>();
    private int currentIndex = 0;

    // 문제 세트를 새로 저장
    public synchronized void setQuestions(List<String> newQuestions) {
        questions.clear();
        questions.addAll(newQuestions);
        currentIndex = 0;
    }

    // 다음 문제를 반환
    public synchronized String getNextQuestion() {
        if (questions.isEmpty()) return null;
        if (currentIndex >= questions.size()) return null;

        String question = questions.get(currentIndex);
        currentIndex++;
        return question;
    }

    // 남은 문제가 있는지 확인
    public synchronized boolean hasRemainingQuestions() {
        return currentIndex < questions.size();
    }
}
