package com.example.sbb.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/document")
public class UploadController {

    // ✅ 프로젝트 루트 기준으로 절대경로 지정 (menhaera/uploads)
    private static final String DIR =
            System.getProperty("user.dir") + File.separator + "uploads";

    @GetMapping("/upload")
    public String form() {
        return "document_upload"; // templates/document_upload.html
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("pdfFile") MultipartFile file) {
        try {
            // 1️⃣ 업로드 파일 확인
            if (file == null || file.isEmpty()) {
                System.out.println("❌ 업로드된 파일이 없습니다.");
                return "redirect:/document/upload";
            }

            // 2️⃣ uploads 폴더 생성
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
            String name = System.currentTimeMillis() + "_" + originalName;

            // 4️⃣ 파일 저장 경로 설정
            File dest = new File(dir, name);
            file.transferTo(dest);

            System.out.println("✅ 파일 업로드 완료");
            System.out.println("   - 원본 이름: " + originalName);
            System.out.println("   - 저장 경로: " + dest.getAbsolutePath());

            // 5️⃣ 업로드 후 홈으로 이동
            return "redirect:/";

        } catch (Exception e) {
            // 6️⃣ 에러 발생 시 서버 종료 없이 로그만 출력
            System.out.println("❌ 업로드 중 에러: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/document/upload";
        }
    }
}
