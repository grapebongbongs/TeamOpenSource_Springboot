package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.document.DocumentFileRepository;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/document")
public class UploadController {

    // ✅ 프로젝트 루트 기준으로 절대경로 지정 (menhaera/uploads)
    private static final String DIR =
            System.getProperty("user.dir") + File.separator + "uploads";

    private final DocumentFileRepository documentFileRepository;
    private final UserService userService;

    // 생성자 주입
    public UploadController(DocumentFileRepository documentFileRepository,
                            UserService userService) {
        this.documentFileRepository = documentFileRepository;
        this.userService = userService;
    }

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
            // 로그인 체크 (원하면 SecurityConfig에서 인증 필수로 막아도 됨)
            if (principal == null) {
                System.out.println("❌ 로그인 안 된 상태에서 업로드 요청");
                return "redirect:/user/login";
            }

            // 1️⃣ 업로드 파일 확인
            if (file == null || file.isEmpty()) {
                System.out.println("❌ 업로드된 파일이 없습니다.");
                return "redirect:/document/upload";
            }

            // 2️⃣ 업로드 폴더 준비
            File dir = new File(DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("📁 uploads 폴더 생성 시도: " + dir.getAbsolutePath());
                if (!created) {
                    throw new IOException("업로드 폴더 생성 실패");
                }
            }

            // 3️⃣ 파일 이름 생성
            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;

            // 4️⃣ 파일 실제 저장
            File dest = new File(dir, storedName);
            file.transferTo(dest);

            System.out.println("✅ 파일 업로드 완료");
            System.out.println("   - 원본 이름: " + originalName);
            System.out.println("   - 저장 경로: " + dest.getAbsolutePath());

            // 5️⃣ 현재 로그인 유저 조회
            SiteUser user = userService.getUser(principal.getName());

            // 6️⃣ DB에 메타데이터 저장
            //    filePath는 나중에 쓸 걸 생각해서 상대경로로 넣어도 되고,
            //    dest.getAbsolutePath()로 절대경로를 넣어도 됨.
            String relativePath = "uploads" + File.separator + storedName;

            DocumentFile doc = new DocumentFile(
                    originalName,
                    storedName,
                    relativePath,          // 또는 dest.getAbsolutePath()
                    file.getSize(),
                    user
            );
            documentFileRepository.save(doc);

            // 7️⃣ 업로드 후 목록 페이지로 이동
            return "redirect:/document/list";

        } catch (Exception e) {
            // 8️⃣ 에러 발생 시 서버 종료 없이 로그만 찍고 다시 폼으로
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
