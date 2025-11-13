/*

package com.example.sbb.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.example.sbb.service.GeminiQuestionService;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/question")
public class GeminiQuestionController {

    private final GeminiQuestionService geminiQuestionService;

    // 새로운 문제 세트를 수동으로 생성할 수도 있음
    @PostMapping("/generate")
    public String generateQuestions(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        geminiQuestionService.generateAndStoreQuestions(text);
        return "✅ 새로운 문제 세트를 생성했습니다!";
    }

    // 다음 문제 하나씩 가져오기
    @GetMapping("/next")
    public String getNextQuestion(@RequestParam(required = false) String text) {
        // text가 없으면 이전에 저장된 문제 세트에서 가져오고,
        // 남은 문제가 없으면 자동으로 새로운 세트를 생성함
        return geminiQuestionService.getNextOrGenerate(text);
    }
}

*/