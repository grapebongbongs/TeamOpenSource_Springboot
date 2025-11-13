package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.DocumentFileRepository;
import com.example.sbb.service.GeminiQuestionService;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
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
    private final GeminiQuestionService geminiQuestionService;
    private final UserService userService;

    // ===========================
    // 업로드 폼
    // ===========================
    @GetMapping("/upload")
    public String form() {
        return "document_upload";
    }

    // ===========================
    // PDF 업로드 + 텍스트 추출 + DB 저장
    // ===========================
    @PostMapping("/upload")
    public String upload(@RequestParam("pdfFile") MultipartFile file,
                         Principal principal,
                         RedirectAttributes rttr) {

        try {
            if (principal == null) return "redirect:/login";
            if (file == null || file.isEmpty()) {
                rttr.addFlashAttribute("error", "파일이 비어 있습니다.");
                return "redirect:/document/upload";
            }

            // ✅ 업로드 폴더 확인/생성
            File dir = new File(DIR);
            if (!dir.exists()) dir.mkdirs();

            // ✅ 파일 이름 설정
            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;
            File dest = new File(dir, storedName);
            file.transferTo(dest);

            // ✅ PDF 텍스트 추출
            String extractedText = extractTextFromPdf(dest);

            // ✅ 로그인 사용자
            SiteUser user = userService.getUser(principal.getName());

            // ✅ DB 저장 (테이블 구조에 맞춤)
            DocumentFile doc = new DocumentFile();
            doc.setOriginalFilename(originalName);
            doc.setUploadedAt(LocalDateTime.now());
            doc.setUser(user);
            doc.setContentText(extractedText);

            documentFileRepository.save(doc);

            rttr.addFlashAttribute("message", "✅ 업로드 및 텍스트 저장 완료!");
            return "redirect:/document/list";

        } catch (Exception e) {
            e.printStackTrace();
            rttr.addFlashAttribute("error", "❌ 업로드 중 오류: " + e.getMessage());
            return "redirect:/document/upload";
        }
    }

    // ===========================
    // 목록 보기
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
    // 삭제 기능 (DB + 실제 파일)
    // ===========================
    @PostMapping("/delete/{id}")
    public String deleteDocument(@PathVariable Long id,
                                 Principal principal,
                                 RedirectAttributes rttr) {

        if (principal == null) return "redirect:/login";

        DocumentFile file = documentFileRepository.findById(id).orElse(null);
        if (file == null) {
            rttr.addFlashAttribute("error", "삭제할 데이터가 없습니다.");
            return "redirect:/document/list";
        }

        // 실제 파일 경로 추정 (optional)
        try {
            Path path = Paths.get(DIR, file.getOriginalFilename());
            if (Files.exists(path)) Files.delete(path);
        } catch (IOException e) {
            rttr.addFlashAttribute("error", "파일 삭제 오류: " + e.getMessage());
            return "redirect:/document/list";
        }

        // ✅ DB에서 삭제
        documentFileRepository.delete(file);
        rttr.addFlashAttribute("message", "🗑 삭제 완료");
        return "redirect:/document/list";
    }

    // ===========================
    // PDF 텍스트 추출 함수
    // ===========================
    private String extractTextFromPdf(File pdfFile) {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (IOException e) {
            System.out.println("⚠ PDF 텍스트 추출 실패: " + e.getMessage());
            return "";
        }
    }


    // ===========================
    // 🧠 DB의 모든 PDF 텍스트로 문제 생성
    // ===========================
    @GetMapping("/makeprob")
    public String makeProblemFromAllPdfs(Model model) {
        List<DocumentFile> allDocs = documentFileRepository.findAll();

        if (allDocs.isEmpty()) {
            model.addAttribute("error", "📂 데이터베이스에 PDF가 없습니다. 먼저 업로드해주세요.");
            return "document_list";
        }

        List<String> texts = new ArrayList<>();
        List<String> names = new ArrayList<>();

        for (DocumentFile doc : allDocs) {
            if (doc.getContentText() != null && !doc.getContentText().isBlank()) {
                texts.add(doc.getContentText());
                names.add(doc.getOriginalFilename());
            }
        }

        if (texts.isEmpty()) {
            model.addAttribute("error", "⚠️ DB에 저장된 텍스트가 없습니다.");
            return "document_list";
        }

        // Gemini API를 통해 문제 생성
        String questions = geminiQuestionService.generateQuestionsFromTexts(texts, names);

        model.addAttribute("originalName", "총 " + names.size() + "개 문서");
        model.addAttribute("questions", questions);
        return "document_makeprob_result";
    }


}
