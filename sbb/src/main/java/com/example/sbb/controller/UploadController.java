package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.DocumentFileRepository;
import com.example.sbb.service.GeminiQuestionService;
import com.example.sbb.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/document")
@RequiredArgsConstructor
public class UploadController {

    private static final String DIR =
            System.getProperty("user.dir") + File.separator + "uploads";

    private final DocumentFileRepository documentFileRepository;
    private final UserService userService;
    private final GeminiQuestionService geminiQuestionService;
    private final QuizService quizService;

    // ===========================
    // 업로드 폼
    // ===========================
    @GetMapping("/upload")
    public String form() {
        return "document_upload";
    }

    // ===========================
    // PDF 업로드 처리
    // ===========================
    @PostMapping("/upload")
    public String upload(@RequestParam("pdfFile") MultipartFile file,
                         Principal principal) {

        try {
            if (principal == null) return "redirect:/login";
            if (file == null || file.isEmpty()) return "redirect:/document/upload";

            File dir = new File(DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;

            File dest = new File(dir, storedName);
            file.transferTo(dest);

            SiteUser user = userService.getUser(principal.getName());
            String relativePath = "uploads" + File.separator + storedName;

            DocumentFile doc = new DocumentFile(
                    originalName, storedName, relativePath, file.getSize(), user
            );
            doc.setUploadedAt(LocalDateTime.now());

            documentFileRepository.save(doc);

            return "redirect:/document/list";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/document/upload";
        }
    }

    // ===========================
    // 내 PDF 목록
    // ===========================
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        List<DocumentFile> files = documentFileRepository.findByUser(user);

        model.addAttribute("files", files);
        return "document_list";
    }

    // ===========================
    // PDF 삭제
    // ===========================
    @PostMapping("/delete/{id}")
    public String deleteDocument(@PathVariable Long id,
                                 Principal principal,
                                 RedirectAttributes rttr) {

        if (principal == null) return "redirect:/login";

        DocumentFile file = documentFileRepository.findById(id).orElse(null);
        if (file == null) {
            rttr.addFlashAttribute("error", "삭제할 파일이 없습니다.");
            return "redirect:/document/list";
        }

        try {
            Path path = Paths.get(System.getProperty("user.dir"), "uploads", file.getStoredFilename());
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception e) {
            rttr.addFlashAttribute("error", "파일 삭제 오류: " + e.getMessage());
            return "redirect:/document/list";
        }

        documentFileRepository.delete(file);
        rttr.addFlashAttribute("message", "🗑 삭제되었습니다.");
        return "redirect:/document/list";
    }

    // ===========================
    // 🔥 리스트에 있는 모든 PDF 기반으로 문제 생성
    // ===========================
    @GetMapping("/makeprob")
    public String makeProblemFromList(Principal principal, Model model) {

        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        List<DocumentFile> files = documentFileRepository.findByUser(user);

        if (files.isEmpty()) {
            model.addAttribute("error", "PDF가 존재하지 않습니다. 먼저 업로드해 주세요.");
            model.addAttribute("files", files);
            return "document_list";
        }

        List<byte[]> pdfBytesList = new ArrayList<>();
        List<String> names = new ArrayList<>();

        try {
            for (DocumentFile file : files) {
                Path path = Paths.get(System.getProperty("user.dir"), "uploads", file.getStoredFilename());
                byte[] bytes = Files.readAllBytes(path);

                pdfBytesList.add(bytes);
                names.add(file.getOriginalFilename());
            }
        } catch (Exception e) {
            model.addAttribute("error", "PDF 읽기 오류: " + e.getMessage());
            model.addAttribute("files", files);
            return "document_list";
        }

        // 1) Gemini에게 여러 PDF를 보내서 "문제 텍스트" 생성
        String rawQuestions =
                geminiQuestionService.generateQuestionsFromMultiplePdfs(pdfBytesList, names);

        // 2) 그 텍스트를 파싱해서 QuizQuestion 엔티티로 저장
        List<QuizQuestion> savedQuestions =
                quizService.saveFromRawText(rawQuestions, user, files);

        // 3) 결과 화면으로 전달
        model.addAttribute("originalName", "총 " + names.size() + "개 문서");
        model.addAttribute("questionsRaw", rawQuestions);
        model.addAttribute("savedCount", savedQuestions.size());

        return "document_makeprob_result";
    }
}
