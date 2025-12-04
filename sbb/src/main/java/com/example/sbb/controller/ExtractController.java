package com.example.sbb.controller;

import com.example.sbb.domain.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
@RequestMapping("/document")
public class ExtractController {
  private final DocumentService documentService;

  @GetMapping("/extract")
  @ResponseBody
  public String extract(@RequestParam String file) throws Exception {
    return documentService.extractText(Paths.get(System.getProperty("user.dir"), "uploads", file));
  }
}
