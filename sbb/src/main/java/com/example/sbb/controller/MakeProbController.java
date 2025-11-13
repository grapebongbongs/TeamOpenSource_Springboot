package com.example.sbb.controller;

import com.example.sbb.service.GeminiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/document")
public class MakeProbController {

    private final GeminiQuestionService geminiQuestionService;

    /**
     * 문제 생성 페이지 (파일 업로드 화면)
     */
    @GetMapping("/makeprob")
    public String showMakeProb(Model model) {
        return "document_makeprob";   // 파일 업로드 페이지
    }

    /**
     * PDF 파일 업로드 → Gemini 파일 기반 문제 생성
     */
    @PostMapping("/makeprob")
    public String makeProblem(@RequestParam("pdfFile") MultipartFile file,
                              Model model) {

        try {
            // 1) 파일 비어있는지 확인
            if (file == null || file.isEmpty()) {
                model.addAttribute("error", "❗ PDF 파일이 업로드되지 않았습니다.");
                return "document_makeprob";
            }

            // 2) Gemini API로 PDF 기반 문제 생성
            byte[] pdfBytes = file.getBytes();
            String originalName = file.getOriginalFilename();

            // ⭐ Gemini 파일 기반 문제 생성 메서드 호출
            String questions = geminiQuestionService.generateQuestionsFromPdf(pdfBytes, originalName);

            // 3) 결과 화면으로 전달
            model.addAttribute("originalName", originalName);
            model.addAttribute("questions", questions);

            return "document_makeprob_result";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "❗ 문제 생성 중 오류 발생: " + e.getMessage());
            return "document_makeprob";
        }
    }
}
