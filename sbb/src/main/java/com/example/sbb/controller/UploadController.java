package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.DocumentFileRepository;
import com.example.sbb.service.GeminiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

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

    // 업로드 경로
    private static final String DIR =
            System.getProperty("user.dir") + File.separator + "uploads";

    private final DocumentFileRepository documentFileRepository;
    private final UserService userService;
    private final GeminiQuestionService geminiQuestionService;

    // ===========================
    // 업로드 FORM
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

            // 업로드 폴더 생성
            File dir = new File(DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;

            File dest = new File(dir, storedName);
            file.transferTo(dest);

            // DB 저장
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
    // PDF 목록 보기
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

        // 실제 파일 삭제
        try {
            Path path = Paths.get(System.getProperty("user.dir"), "uploads", file.getStoredFilename());

            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception e) {
            rttr.addFlashAttribute("error", "파일 삭제 오류: " + e.getMessage());
            return "redirect:/document/list";
        }

        // DB 삭제
        documentFileRepository.delete(file);

        rttr.addFlashAttribute("message", "🗑 삭제되었습니다.");
        return "redirect:/document/list";
    }


    // ===========================
    // 📌 리스트에 있는 PDF 전부를 이용하여 즉시 문제 생성
    // ===========================
    @GetMapping("/makeprob")
    public String makeProblemFromList(Principal principal, Model model) {

        if (principal == null) return "redirect:/login";

        SiteUser user = userService.getUser(principal.getName());
        List<DocumentFile> files = documentFileRepository.findByUser(user);

        if (files.isEmpty()) {
            model.addAttribute("error", "PDF가 존재하지 않습니다. 먼저 업로드해 주세요.");
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
            return "document_list";
        }

        // Gemini 문제 생성
        String questions =
                geminiQuestionService.generateQuestionsFromMultiplePdfs(pdfBytesList, names);

        model.addAttribute("originalName", "총 " + names.size() + "개 문서");
        model.addAttribute("questions", questions);

        return "document_makeprob_result";
    }
}
