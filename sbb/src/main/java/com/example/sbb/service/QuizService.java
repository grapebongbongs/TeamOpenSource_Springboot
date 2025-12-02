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
                                              List<DocumentFile> sourceDocs,
                                              com.example.sbb.domain.Folder defaultFolder) {

        List<QuizQuestion> result = new ArrayList<>();

        if (rawText == null || rawText.isBlank()) {
            return result;
        }

        String[] lines = rawText.split("\\R"); // 개행 기준 split
        QuizQuestion current = null;
        StringBuilder questionBuffer = new StringBuilder();
        StringBuilder choiceBuffer = new StringBuilder();
        boolean readingChoices = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // [번호]. 문제 내용  예: [1]. 두 집단의 ...
            if (trimmed.matches("^\\[\\d+].*")) {
                // 이전 문제 마무리
                if (current != null) {
                    finalizeQuestion(current, questionBuffer, choiceBuffer, result);
                }

                current = new QuizQuestion();
                current.setUser(user);

                if (sourceDocs != null && !sourceDocs.isEmpty()) {
                    DocumentFile firstDoc = sourceDocs.get(0);
                    current.setDocument(firstDoc);
                    current.setFolder(firstDoc.getFolder());
                } else if (defaultFolder != null) {
                    current.setFolder(defaultFolder);
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
                readingChoices = false;
                choiceBuffer.setLength(0);
                questionBuffer.append(afterBracket).append(" ");

            } else if (trimmed.startsWith("(보기)")) {
                if (current != null) {
                    current.setMultipleChoice(true);
                    choiceBuffer.setLength(0);
                    String choicePart = trimmed.replaceFirst("^\\(보기\\)\\s*", "");
                    choiceBuffer.append(choicePart);
                    if (!choicePart.endsWith("\n")) {
                        choiceBuffer.append("\n");
                    }
                    readingChoices = true;
                }
            } else if (trimmed.startsWith("[정답]")) {
                if (current != null) {
                    if (choiceBuffer.length() > 0) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    String ans = trimmed.replaceFirst("^\\[정답]\\s*", "");
                    current.setAnswer(ans);
                }
                readingChoices = false;
            } else if (trimmed.startsWith("[해설]")) {
                if (current != null) {
                    if (choiceBuffer.length() > 0 && current.getChoices() == null) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    String exp = trimmed.replaceFirst("^\\[해설]\\s*", "");
                    current.setExplanation(exp);
                }
                readingChoices = false;
            } else if (readingChoices && current != null) {
                choiceBuffer.append(trimmed);
                if (!trimmed.endsWith("\n")) {
                    choiceBuffer.append("\n");
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
            finalizeQuestion(current, questionBuffer, choiceBuffer, result);
        }

        return result;
    }

    private void finalizeQuestion(QuizQuestion current,
                                  StringBuilder questionBuffer,
                                  StringBuilder choiceBuffer,
                                  List<QuizQuestion> result) {
        current.setQuestionText(questionBuffer.toString().trim());
        if (choiceBuffer.length() > 0 && (current.getChoices() == null || current.getChoices().isBlank())) {
            current.setMultipleChoice(true);
            current.setChoices(choiceBuffer.toString().trim());
        }
        quizQuestionRepository.save(current);
        result.add(current);
        questionBuffer.setLength(0);
        choiceBuffer.setLength(0);
    }
}
