package com.example.sbb.controller;

import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import com.example.sbb.domain.user.UserService;
import com.example.sbb.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderRepository folderRepository;
    private final UserService userService;

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
}
