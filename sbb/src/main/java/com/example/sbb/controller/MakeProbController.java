package com.example.sbb.controller;

import com.example.sbb.domain.document.Document;
import com.example.sbb.service.GeminiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/document")
public class MakeProbController {

    private final GeminiQuestionService geminiQuestionService;

    @GetMapping("/makeprob")
    public String showMakeProb(Model model) {
        model.addAttribute("document", new Document());
        model.addAttribute("message", "문제 만들기 페이지");
        return "document_makeprob";
    }

    @PostMapping("/makeprob")
    public String makeProblem(@ModelAttribute("document") Document doc,
                              Model model) {

        if (doc.getExtracted() == null || doc.getExtracted().isBlank()) {
            model.addAttribute("error", "❗ 텍스트가 비어 있습니다.");
            model.addAttribute("document", doc);
            return "document_makeprob";
        }

        String questions = geminiQuestionService.generateQuestionsFromText(doc.getExtracted());

        model.addAttribute("originalName", doc.getOriginalName());
        model.addAttribute("questions", questions);

        return "document_makeprob_result";
    }
}
