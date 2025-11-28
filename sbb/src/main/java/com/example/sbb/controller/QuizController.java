package com.example.sbb.controller;

import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.QuizQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizQuestionRepository quizQuestionRepository;
    private final UserService userService;

    // 전체 퀴즈 목록(간단히)
  @GetMapping("/list")
public String list(Model model,
                   Principal principal,
                   @RequestParam(value = "page", defaultValue = "0") int page) {
    if (principal == null) return "redirect:/login";

    // TODO: JPA 로직 전부 잠시 주석 처리
    
    SiteUser user = userService.getUser(principal.getName());

    int pageIndex = Math.max(page, 0);
        Pageable pageable = PageRequest.of(pageIndex, 10, Sort.by("createdAt").ascending());
        Page<QuizQuestion> pageData = quizQuestionRepository.findByUserOrderByCreatedAtAsc(user, pageable);

    int totalPages = pageData.getTotalPages() == 0 ? 1 : pageData.getTotalPages();

    model.addAttribute("questions", pageData.getContent());
    model.addAttribute("currentPage", pageData.getNumber());
    model.addAttribute("totalPages", totalPages);
    model.addAttribute("hasPrevious", pageData.hasPrevious());
    model.addAttribute("hasNext", pageData.hasNext());
    model.addAttribute("totalElements", pageData.getTotalElements());
    

    return "quiz_list";
}


    @GetMapping("/next")
    public String goToNext(Principal principal,
                           RedirectAttributes rttr) {
        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        QuizQuestion next = findNextPendingQuestion(user);

        if (next == null) {
            rttr.addFlashAttribute("message", "풀 문제를 모두 완료했습니다.");
            return "redirect:/quiz/list";
        }
        return "redirect:/quiz/solve/" + next.getId();
    }

    // 문제 풀기 화면
    @GetMapping("/solve/{id}")
    public String showQuiz(@PathVariable Long id,
                           Principal principal,
                           Model model) {
        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        QuizQuestion q = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        if (!q.getUser().getId().equals(user.getId())) {
            return "redirect:/quiz/list";
        }

        model.addAttribute("question", q);
        model.addAttribute("choiceList", extractChoices(q.getChoices()));
        return "quiz_solve";
    }

    // 답 제출
    @PostMapping("/solve/{id}")
    public String submitQuiz(@PathVariable Long id,
                             @RequestParam("answer") String userAnswer,
                             Principal principal,
                             Model model) {
        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        QuizQuestion q = quizQuestionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문제를 찾을 수 없습니다."));

        if (!q.getUser().getId().equals(user.getId())) {
            return "redirect:/quiz/list";
        }

        String normalizedUser = normalizeAnswerText(userAnswer);
        String normalizedCorrect = normalizeAnswerText(q.getAnswer());

        boolean isCorrect = !normalizedCorrect.isEmpty()
                && normalizedCorrect.equals(normalizedUser);

        q.setSolved(true);
        q.setCorrect(isCorrect);
        quizQuestionRepository.save(q);

        model.addAttribute("question", q);
        model.addAttribute("userAnswer", userAnswer);
        model.addAttribute("correct", isCorrect);
        model.addAttribute("choiceList", extractChoices(q.getChoices()));
        model.addAttribute("normalizedCorrectAnswer", normalizedCorrect);
        model.addAttribute("normalizedUserAnswer", normalizedUser);

        QuizQuestion next = findNextPendingQuestion(user);
        if (next != null) {
            model.addAttribute("nextQuestionId", next.getId());
        }

        return "quiz_result";
    }

    private List<ChoiceOption> extractChoices(String rawChoices) {
        List<ChoiceOption> result = new ArrayList<>();
        if (rawChoices == null || rawChoices.isBlank()) {
            return result;
        }

        Pattern pattern = Pattern.compile("(\\d+)[\\).]\\s*(.*?)(?=(?:\\s*\\d+[\\).])|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(rawChoices);
        while (matcher.find()) {
            String prefix = matcher.group(1).trim();
            String text = matcher.group(2).trim();
            if (!text.isEmpty()) {
                result.add(new ChoiceOption(prefix, text));
            }
        }

        if (result.isEmpty()) {
            String[] chunks = rawChoices.split("[\\n\\r,;]+");
            for (String chunk : chunks) {
                String trimmed = chunk.trim();
                if (!trimmed.isEmpty()) {
                    result.add(new ChoiceOption(null, trimmed));
                }
            }
        }

        return result;
    }

    private QuizQuestion findNextPendingQuestion(SiteUser user) {
        return quizQuestionRepository
                .findByUserAndSolvedFalseOrderByCreatedAtAsc(user)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^A-Za-z0-9가-힣]");

    private static String normalizeAnswerText(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return "";
        return NORMALIZE_PATTERN.matcher(trimmed).replaceAll("").toLowerCase();
    }

    public static class ChoiceOption {
        private final String number;
        private final String text;
        private final String value;
        private final String normalizedValue;

        public ChoiceOption(String number, String text) {
            this.number = number;
            this.text = text;
            this.value = (number != null && !number.isBlank())
                    ? number.trim()
                    : text;
            this.normalizedValue = normalizeAnswerText(this.value);
        }

        public String getNumber() {
            return number;
        }

        public String getText() {
            return text;
        }

        public String getValue() {
            return value;
        }

        public String getNormalizedValue() {
            return normalizedValue;
        }
    }
}
