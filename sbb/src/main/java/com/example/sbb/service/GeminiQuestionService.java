package com.example.sbb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiQuestionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-pro}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateQuestionsFromText(String text) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        String prompt = """
                아래 텍스트를 기반으로 객관식 3문제 + 주관식 2문제를 만들어줘.
                각 문제에는 정답과 간단한 해설도 포함해줘.

                텍스트:
                """ + text;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map data = response.getBody();
            List candidates = (List) data.get("candidates");
            Map first = (Map) candidates.get(0);
            Map content = (Map) first.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);

            return firstPart.get("text").toString();

        } catch (Exception e) {
            return "⚠ Gemini 오류: " + e.getMessage();
        }
    }
}
