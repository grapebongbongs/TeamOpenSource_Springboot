package com.example.sbb.service;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository quizQuestionRepository;

    /**
     * Gemini가 반환한 전체 텍스트를 파싱해서
     * QuizQuestion 엔티티로 저장
     */
    public List<QuizQuestion> saveFromRawText(String rawText,
                                              SiteUser user,
                                              List<DocumentFile> sourceDocs) {

        List<QuizQuestion> result = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            return result;
        }

        String[] lines = rawText.split("\\R"); // 개행 기준 split
        QuizQuestion current = null;
        StringBuilder questionBuffer = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();

            // [번호]. 문제 내용  예: [1]. 두 집단의 ...
            if (trimmed.matches("^\\[\\d+].*")) {
                // 이전 문제 마무리
                if (current != null) {
                    current.setQuestionText(questionBuffer.toString().trim());
                    quizQuestionRepository.save(current);
                    result.add(current);
                }

                current = new QuizQuestion();
                current.setUser(user);

                if (sourceDocs != null && !sourceDocs.isEmpty()) {
                    // 여러 PDF를 기반으로 했으니 일단 첫 번째나 null로 둘 수 있음
                    current.setDocument(sourceDocs.get(0));
                }

                questionBuffer.setLength(0);

                // 번호 추출
                String numStr = trimmed.replaceFirst("^\\[(\\d+)].*", "$1");
                try {
                    current.setNumberTag(Integer.parseInt(numStr));
                } catch (NumberFormatException e) {
                    current.setNumberTag(null);
                }

                // 문제 텍스트 부분 ("]" 뒤로)
                String afterBracket = trimmed.replaceFirst("^\\[\\d+].\\s*", "");
                questionBuffer.append(afterBracket).append(" ");

            } else if (trimmed.startsWith("(보기)")) {
                if (current != null) {
                    current.setMultipleChoice(true);
                    // "(보기)" 제거 후 보기를 통째로 저장
                    String choicePart = trimmed.replaceFirst("^\\(보기\\)\\s*", "");
                    current.setChoices(choicePart);
                }
            } else if (trimmed.startsWith("[정답]")) {
                if (current != null) {
                    String ans = trimmed.replaceFirst("^\\[정답]\\s*", "");
                    current.setAnswer(ans);
                }
            } else if (trimmed.startsWith("[해설]")) {
                if (current != null) {
                    String exp = trimmed.replaceFirst("^\\[해설]\\s*", "");
                    current.setExplanation(exp);
                }
            } else {
                // 기타 줄은 문제 본문에 이어 붙임
                if (current != null) {
                    questionBuffer.append(trimmed).append(" ");
                }
            }
        }

        // 마지막 문제 마무리
        if (current != null) {
            current.setQuestionText(questionBuffer.toString().trim());
            quizQuestionRepository.save(current);
            result.add(current);
        }

        return result;
    }
}
