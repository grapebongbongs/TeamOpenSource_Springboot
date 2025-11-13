package com.example.sbb.controller;

import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizQuestionRepository quizQuestionRepository;
    private final UserService userService;

    // 전체 퀴즈 목록(간단히)
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        List<QuizQuestion> pending = quizQuestionRepository
                .findByUserAndSolvedFalseOrderByCreatedAtAsc(user);

        List<QuizQuestion> wrong = quizQuestionRepository
                .findByUserAndSolvedTrueAndCorrectFalseOrderByCreatedAtAsc(user);

        List<QuizQuestion> correct = quizQuestionRepository
                .findByUserAndSolvedTrueAndCorrectTrueOrderByCreatedAtAsc(user);

        model.addAttribute("pendingQuestions", pending);
        model.addAttribute("wrongQuestions", wrong);
        model.addAttribute("correctQuestions", correct);

        return "quiz_list";
    }

    // 문제 풀기 화면
    @GetMapping("/solve/{id}")
    public String showQuiz(@PathVariable Long id,
                           Principal principal,
                           Model model) {
        if (principal == null) return "redirect:/login";

        QuizQuestion q = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        model.addAttribute("question", q);
        return "quiz_solve";
    }

    // 답 제출
    @PostMapping("/solve/{id}")
    public String submitQuiz(@PathVariable Long id,
                             @RequestParam("answer") String userAnswer,
                             Principal principal,
                             Model model) {
        if (principal == null) return "redirect:/login";

        QuizQuestion q = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        boolean isCorrect = false;
        if (q.getAnswer() != null) {
            isCorrect = userAnswer.trim()
                    .equalsIgnoreCase(q.getAnswer().trim());
        }

        q.setSolved(true);
        q.setCorrect(isCorrect);
        quizQuestionRepository.save(q);

        model.addAttribute("question", q);
        model.addAttribute("userAnswer", userAnswer);
        model.addAttribute("correct", isCorrect);

        return "quiz_result";
    }
}
