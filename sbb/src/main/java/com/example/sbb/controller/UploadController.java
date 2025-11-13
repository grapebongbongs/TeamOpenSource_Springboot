package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.DocumentFileRepository;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/document")
@RequiredArgsConstructor
public class UploadController {

    // ✅ 프로젝트 루트 기준 uploads 폴더 (TeamOpenSource_Springboot/sbb/uploads)
    private static final String DIR =
            System.getProperty("user.dir") + File.separator + "uploads";

    private final DocumentFileRepository documentFileRepository;
    private final UserService userService;

    // ===========================
    // 업로드 폼
    // ===========================
    @GetMapping("/upload")
    public String form() {
        return "document_upload"; // templates/document_upload.html
    }

    // ===========================
    // PDF 업로드 처리 + DB 저장
    // ===========================
    @PostMapping("/upload")
    public String upload(@RequestParam("pdfFile") MultipartFile file,
                         Principal principal) {

        try {
            // 0) 로그인 체크
            if (principal == null) {
                System.out.println("❌ 로그인 안 된 상태에서 업로드 요청");
                return "redirect:/user/login";
            }

            // 1) 파일 체크
            if (file == null || file.isEmpty()) {
                System.out.println("❌ 업로드된 파일이 없습니다.");
                return "redirect:/document/upload";
            }

            // 2) 업로드 폴더 준비
            File dir = new File(DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("업로드 폴더 생성 실패: " + dir.getAbsolutePath());
            }

            // 3) 파일 이름 및 경로 설정
            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;
            File dest = new File(dir, storedName);

            // 4) 실제 파일 저장
            file.transferTo(dest);

            System.out.println("✅ 파일 업로드 완료");
            System.out.println("   - 원본 이름: " + originalName);
            System.out.println("   - 저장 경로: " + dest.getAbsolutePath());

            // 5) 로그인 유저 조회
            SiteUser user = userService.getUser(principal.getName());

            // 6) DB에 메타데이터 저장
            String relativePath = "uploads" + File.separator + storedName;

            DocumentFile doc = new DocumentFile();
            doc.setOriginalFilename(originalName);
            doc.setStoredFilename(storedName);
            doc.setFilePath(relativePath);          // 또는 dest.getAbsolutePath()
            doc.setFileSize(file.getSize());
            doc.setUploadedAt(LocalDateTime.now());
            doc.setUser(user);

            documentFileRepository.save(doc);

            // 7) 업로드 후 목록으로 이동
            return "redirect:/document/list";

        } catch (Exception e) {
            System.out.println("❌ 업로드 중 에러: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/document/upload";
        }
    }

    // ===========================
    // 내 PDF 목록 보기
    // ===========================
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/user/login";
        }

        SiteUser user = userService.getUser(principal.getName());
        List<DocumentFile> files = documentFileRepository.findByUser(user);

        model.addAttribute("files", files);
        return "document_list"; // templates/document_list.html
    }
}
