package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentText;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.DocumentTextRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/document")
public class UploadController {

    private final UserService userService;
    private final DocumentTextRepository documentTextRepository;

    // ===========================
    // 업로드 폼
    // ===========================
    @GetMapping("/upload")
    public String uploadForm() {
        return "document_upload"; // templates/document_upload.html
    }

    // ===========================
    // PDF 업로드 + 텍스트 추출 + DB 저장
    // ===========================
    @PostMapping("/upload")
    public String upload(@RequestParam("pdfFile") MultipartFile file, Principal principal) {
        try {
            if (principal == null) {
                System.out.println("❌ 로그인되지 않음");
                return "redirect:/user/login";
            }

            if (file.isEmpty()) {
                System.out.println("❌ 파일이 비어 있음");
                return "redirect:/document/upload";
            }

            // 1️⃣ PDF 임시 저장
            File tempPdf = File.createTempFile("upload_", ".pdf");
            file.transferTo(tempPdf);

            // 2️⃣ PDFBox로 텍스트 추출
            String extractedText = extractTextFromPdf(tempPdf);

            // 3️⃣ 로그인 사용자
            SiteUser user = userService.getUser(principal.getName());

            // 4️⃣ DB에 저장
            DocumentText doc = new DocumentText();
            doc.setOriginalFilename(file.getOriginalFilename());
            doc.setUploadedAt(LocalDateTime.now());
            doc.setUser(user);
            doc.setContentText(extractedText);

            DocumentText saved = documentTextRepository.save(doc);

            System.out.println("✅ 저장 완료 → id=" + saved.getId()
                    + ", 파일명=" + file.getOriginalFilename()
                    + ", 텍스트 길이=" + (extractedText == null ? 0 : extractedText.length()));

            tempPdf.delete(); // 임시 파일 삭제
            return "redirect:/document/list";

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ 업로드/저장 실패: " + e.getMessage());
            return "redirect:/document/upload";
        }
    }

    // ===========================
    // 업로드된 PDF 목록 보기
    // ===========================
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        if (principal == null) return "redirect:/user/login";

        SiteUser user = userService.getUser(principal.getName());
        List<DocumentText> docs = documentTextRepository.findByUser(user);

        model.addAttribute("docs", docs);
        return "document_list";
    }

    // ===========================
    // PDF 텍스트 추출 함수 (PDFBox)
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
    
}
