package com.example.sbb.controller;

import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.repository.FolderRepository;
import com.example.sbb.repository.DocumentFileRepository;
import com.example.sbb.repository.QuizQuestionRepository;
import com.example.sbb.repository.ProblemRepository;
import com.example.sbb.repository.GroupSharedQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderRepository folderRepository;
    private final UserService userService;
    private final DocumentFileRepository documentFileRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final ProblemRepository problemRepository;
    private final GroupSharedQuestionRepository sharedQuestionRepository;

    @GetMapping
    public String list(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        List<Folder> folders = folderRepository.findByUserOrderByCreatedAtAsc(user);
        model.addAttribute("folders", folders);
        return "folder_list";
    }

    @PostMapping
    public String create(@RequestParam("name") String name,
                         Principal principal,
                         Model model) {
        if (principal == null) return "redirect:/login";
        if (name == null || name.trim().isEmpty()) {
            model.addAttribute("error", "폴더 이름을 입력해 주세요.");
            return list(model, principal);
        }

        SiteUser user = userService.getUser(principal.getName());
        Folder folder = new Folder();
        folder.setName(name.trim());
        folder.setUser(user);
        folderRepository.save(folder);

        return "redirect:/folders";
    }

    @PostMapping("/delete")
    @Transactional
    public String delete(@RequestParam("folderId") Long folderId,
                         Principal principal,
                         Model model) {
        if (principal == null) return "redirect:/login";
        SiteUser user = userService.getUser(principal.getName());
        Folder folder = folderRepository.findByIdAndUser(folderId, user).orElse(null);
        if (folder == null) {
            model.addAttribute("error", "삭제할 폴더를 찾을 수 없습니다.");
            return list(model, principal);
        }

        List<DocumentFile> docs = documentFileRepository.findByFolder(folder);
        for (DocumentFile doc : docs) {
            try {
                Path path = Paths.get(System.getProperty("user.dir"), "uploads", doc.getStoredFilename());
                Files.deleteIfExists(path);
            } catch (Exception ignored) {}
            problemRepository.deleteAllByDocumentFile(doc);
            quizQuestionRepository.deleteAllByDocument(doc);
            documentFileRepository.delete(doc);
        }
        List<QuizQuestion> folderQuestions = quizQuestionRepository.findByFolder(folder);
        if (!folderQuestions.isEmpty()) {
            List<Long> questionIds = folderQuestions.stream().map(QuizQuestion::getId).toList();
            sharedQuestionRepository.deleteAllByQuestion_IdIn(questionIds);
            quizQuestionRepository.deleteAll(folderQuestions);
        }
        folderRepository.delete(folder);
        return "redirect:/folders";
    }
}
