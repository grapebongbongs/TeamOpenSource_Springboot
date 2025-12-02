package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.document.DocumentService;
import com.example.sbb.domain.Folder;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.DocumentFileRepository;
import com.example.sbb.repository.FolderRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.service.GeminiQuestionService;
import com.example.sbb.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

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

    // 프로젝트 루트 기준 uploads 폴더
    private static final String DIR =
            System.getProperty("user.dir") + File.separator + "uploads";

    private final DocumentFileRepository documentFileRepository;
    private final UserService userService;
    private final GeminiQuestionService geminiQuestionService;
    private final QuizService quizService;
    private final QuizQuestionRepository quizQuestionRepository;
    private final DocumentService documentService;
    private final FolderRepository folderRepository;

    // ===========================
    // 업로드 폼
    // ===========================
    @GetMapping("/upload")
    public String form(@RequestParam(value = "folderId", required = false) Long folderId,
                       Model model,
                       Principal principal) {
        if (principal == null) {
            return "redirect:/user/login";
        }

        SiteUser user = userService.getUser(principal.getName());
        model.addAttribute("folders", folderRepository.findByUserOrderByCreatedAtAsc(user));
        model.addAttribute("selectedFolderId", folderId);
        return "document_upload";
    }

    // ===========================
    // PDF 업로드 처리
    // ===========================
    @PostMapping("/upload")
    public String upload(@RequestParam("pdfFile") MultipartFile file,
                         @RequestParam(value = "folderId", required = false) Long folderId,
                         Principal principal) {

        try {
            // 로그인 체크
            if (principal == null) {
                return "redirect:/user/login";
            }

            // 파일 체크
            if (file == null || file.isEmpty()) {
                return "redirect:/document/upload";
            }

            // 업로드 폴더 준비
            File dir = new File(DIR);
            if (!dir.exists()) dir.mkdirs();

            // 파일명 구성
            String originalName = file.getOriginalFilename();
            String storedName = System.currentTimeMillis() + "_" + originalName;
            byte[] fileBytes = file.getBytes();

            // 실제 저장
            File dest = new File(dir, storedName);
            file.transferTo(dest);

            // 현재 유저 조회
            SiteUser user = userService.getUser(principal.getName());
            Folder selectedFolder = null;
            if (folderId != null) {
                selectedFolder = folderRepository.findByIdAndUser(folderId, user).orElse(null);
            }

            // DocumentFile 엔티티 생성 및 저장
            DocumentFile doc = new DocumentFile(
                    originalName,
                    storedName,
                    file.getSize(),
                    user
            );
            doc.setUploadedAt(LocalDateTime.now());
            if (selectedFolder != null) {
                doc.setFolder(selectedFolder);
            }

            DocumentFile saved = documentFileRepository.save(doc);

            // PDF 텍스트 추출 → 스케줄러가 활용할 수 있도록 DB에 저장
            try {
                String extracted = documentService.extractText(fileBytes);
                saved.setExtractedText(extracted);
                documentFileRepository.save(saved);
            } catch (Exception extractEx) {
                extractEx.printStackTrace();
            }

            String redirectSuffix = (selectedFolder != null) ? "?folderId=" + selectedFolder.getId() : "";
            return "redirect:/document/list" + redirectSuffix;

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/document/upload";
        }
    }

    // ===========================
    // 내 PDF 목록
    // ===========================
    @GetMapping("/list")
    public String list(@RequestParam(value = "folderId", required = false) Long folderId,
                       Model model,
                       Principal principal) {

        if (principal == null) {
            return "redirect:/user/login";
        }

        SiteUser user = userService.getUser(principal.getName());
        Folder selectedFolder = null;
        if (folderId != null) {
            selectedFolder = folderRepository.findByIdAndUser(folderId, user).orElse(null);
        }

        List<DocumentFile> files = (selectedFolder != null)
                ? documentFileRepository.findByUserAndFolder(user, selectedFolder)
                : documentFileRepository.findByUser(user);

        model.addAttribute("folders", folderRepository.findByUserOrderByCreatedAtAsc(user));
        model.addAttribute("selectedFolder", selectedFolder);
        model.addAttribute("files", files);
        return "document_list";
    }

    // ===========================
    // PDF 삭제 (관련 퀴즈도 같이 삭제)
    // ===========================
    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteDocument(@PathVariable Long id,
                                 @RequestParam(value = "folderId", required = false) Long folderId,
                                 Principal principal,
                                 RedirectAttributes rttr) {

        if (principal == null) {
            return "redirect:/user/login";
        }

        DocumentFile file = documentFileRepository.findById(id).orElse(null);
        if (file == null) {
            rttr.addFlashAttribute("error", "삭제할 파일이 없습니다.");
            return "redirect:/document/list";
        }

        if (file.getUser() == null ||
                !principal.getName().equals(file.getUser().getUsername())) {
            rttr.addFlashAttribute("error", "삭제 권한이 없습니다.");
            return "redirect:/document/list";
        }

        try {
            // 1) 실제 PDF 파일 삭제
            Path path = Paths.get(System.getProperty("user.dir"), "uploads", file.getStoredFilename());
            Files.deleteIfExists(path);

            // 2) 문서로 생성된 문제 삭제
            quizQuestionRepository.deleteAllByDocument(file);

            // 3) 마지막으로 DocumentFile 삭제
            documentFileRepository.delete(file);

            rttr.addFlashAttribute("message", "🗑 삭제되었습니다.");

        } catch (Exception e) {
            rttr.addFlashAttribute("error", "삭제 중 오류: " + e.getMessage());
            e.printStackTrace();
        }

        String suffix = (file.getFolder() != null) ? "?folderId=" + file.getFolder().getId() : "";
        return "redirect:/document/list" + suffix;
    }

    // ===========================
    // 🔥 리스트에 있는 모든 PDF 기반으로 문제 생성
    // ===========================
    @GetMapping("/makeprob")
    public String makeProblemFromList(@RequestParam(value = "stylePrompt", required = false) String stylePrompt,
                                      @RequestParam(value = "folderId", required = false) Long folderId,
                                      Principal principal,
                                      Model model) {

        if (principal == null) {
            return "redirect:/user/login";
        }

        SiteUser user = userService.getUser(principal.getName());
        Folder selectedFolder = null;
        if (folderId != null) {
            selectedFolder = folderRepository.findByIdAndUser(folderId, user).orElse(null);
        }
        List<DocumentFile> files = (selectedFolder != null)
                ? documentFileRepository.findByUserAndFolder(user, selectedFolder)
                : documentFileRepository.findByUser(user);

        if (files.isEmpty()) {
            model.addAttribute("error", "PDF가 존재하지 않습니다. 먼저 업로드해 주세요.");
            model.addAttribute("files", files);
            model.addAttribute("folders", folderRepository.findByUserOrderByCreatedAtAsc(user));
            model.addAttribute("selectedFolder", selectedFolder);
            model.addAttribute("stylePrompt", stylePrompt);
            return "document_list";
        }

        List<byte[]> pdfBytesList = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> textList = new ArrayList<>();

        for (DocumentFile file : files) {
            try {
                String extracted = file.getExtractedText();
                if (extracted == null || extracted.isBlank()) {
                    Path path = Paths.get(System.getProperty("user.dir"), "uploads", file.getStoredFilename());
                    extracted = documentService.extractText(path);
                }
                if (extracted != null && !extracted.isBlank()) {
                    textList.add(extracted);
                    names.add(file.getOriginalFilename());
                }
            } catch (Exception e) {
                model.addAttribute("error", "텍스트 읽기 오류: " + file.getOriginalFilename() + " - " + e.getMessage());
                model.addAttribute("errorFileId", file.getId());
                model.addAttribute("errorFileName", file.getOriginalFilename());
                model.addAttribute("files", files);
                model.addAttribute("folders", folderRepository.findByUserOrderByCreatedAtAsc(user));
                model.addAttribute("selectedFolder", selectedFolder);
                model.addAttribute("stylePrompt", stylePrompt);
                return "document_list";
            }
        }

        if (textList.isEmpty()) {
            model.addAttribute("error", "PDF 텍스트가 비어 있어 문제를 만들 수 없습니다.");
            model.addAttribute("files", files);
            model.addAttribute("folders", folderRepository.findByUserOrderByCreatedAtAsc(user));
            model.addAttribute("selectedFolder", selectedFolder);
            model.addAttribute("stylePrompt", stylePrompt);
            return "document_list";
        }

        // 1) Gemini에게 저장된 텍스트를 보내서 "문제 텍스트" 생성
        String rawQuestions =
                geminiQuestionService.generateQuestionsFromTexts(textList, names, stylePrompt);

        // 2) 그 텍스트를 파싱해서 QuizQuestion 엔티티로 저장
        List<QuizQuestion> savedQuestions =
                quizService.saveFromRawText(rawQuestions, user, files, selectedFolder);

        // 3) 결과 화면으로 전달
        model.addAttribute("originalName", "총 " + names.size() + "개 문서");
        model.addAttribute("questionsRaw", rawQuestions);
        model.addAttribute("savedCount", savedQuestions.size());
        model.addAttribute("stylePrompt", stylePrompt);
        model.addAttribute("selectedFolder", selectedFolder);

        return "document_makeprob_result";
    }

    // ===========================
    // 강제 삭제 (문제 포함)
    // ===========================
    @PostMapping("/force-delete/{id}")
    @Transactional
    public String forceDeleteDocument(@PathVariable Long id,
                                      @RequestParam(value = "folderId", required = false) Long folderId,
                                      Principal principal,
                                      RedirectAttributes rttr) {

        if (principal == null) {
            return "redirect:/user/login";
        }

        DocumentFile file = documentFileRepository.findById(id).orElse(null);
        if (file == null) {
            rttr.addFlashAttribute("error", "삭제할 파일이 없습니다.");
            return "redirect:/document/list";
        }

        if (file.getUser() == null ||
                !principal.getName().equals(file.getUser().getUsername())) {
            rttr.addFlashAttribute("error", "삭제 권한이 없습니다.");
            return "redirect:/document/list";
        }

        try {
            Path path = Paths.get(System.getProperty("user.dir"), "uploads", file.getStoredFilename());
            Files.deleteIfExists(path);

            quizQuestionRepository.deleteAllByDocument(file);
            documentFileRepository.delete(file);

            rttr.addFlashAttribute("message", "문제와 함께 강제로 삭제했습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("error", "강제 삭제 중 오류: " + e.getMessage());
            e.printStackTrace();
        }

        String suffix = (file.getFolder() != null) ? "?folderId=" + file.getFolder().getId() : "";
        return "redirect:/document/list" + suffix;
    }
}
