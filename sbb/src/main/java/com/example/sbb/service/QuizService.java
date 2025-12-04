package com.example.sbb.service;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizQuestionRepository quizQuestionRepository;
    private static final Pattern QUESTION_LINE_PATTERN =
            Pattern.compile("^\\s*\\[?(\\d+)\\]?\\s*[\\.:\\)\\-]?\\s*(.*)$");
    private static final Pattern ANSWER_LINE_PATTERN =
            Pattern.compile("^\\s*\\[?정답]?\\s*[:\\-\\.]?\\s*(.*)$", Pattern.CASE_INSENSITIVE);

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

            if (readingChoices &&
                    !trimmed.startsWith("[정답]") &&
                    !trimmed.startsWith("[해설]")) {
                if (current != null && !trimmed.isEmpty()) {
                    choiceBuffer.append(trimmed);
                    if (!trimmed.endsWith("\n")) {
                        choiceBuffer.append("\n");
                    }
                }
                continue;
            }

            if (trimmed.startsWith("(보기)")) {
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
            } else if (ANSWER_LINE_PATTERN.matcher(trimmed).matches()) {
                if (current != null) {
                    if (choiceBuffer.length() > 0) {
                        current.setMultipleChoice(true);
                        current.setChoices(choiceBuffer.toString().trim());
                    }
                    Matcher ansMatcher = ANSWER_LINE_PATTERN.matcher(trimmed);
                    if (ansMatcher.find()) {
                        String ans = ansMatcher.group(1).trim();
                        current.setAnswer(ans);
                    }
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
            } else {
                Matcher matcher = QUESTION_LINE_PATTERN.matcher(trimmed);
                if (matcher.matches()) {
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

                    String numStr = matcher.group(1);
                    try {
                        current.setNumberTag(Integer.parseInt(numStr));
                    } catch (NumberFormatException e) {
                        current.setNumberTag(null);
                    }

                    String afterNumber = matcher.group(2);
                    readingChoices = false;
                    choiceBuffer.setLength(0);
                    questionBuffer.append(afterNumber).append(" ");

                } else if (current != null) {
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
        String questionText = questionBuffer.toString().trim();
        if (questionText.isBlank()) {
            questionBuffer.setLength(0);
            choiceBuffer.setLength(0);
            return;
        }
        current.setQuestionText(questionText);
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
