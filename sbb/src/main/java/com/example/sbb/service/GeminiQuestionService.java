package com.example.sbb.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiQuestionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // 기존 gemini-pro → 404 많이 떠서 기본값을 최신 계열로 변경
    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;

    // 간단히 내부에서 생성 (원하면 @Bean으로 주입해도 됨)
    private final RestTemplate restTemplate = new RestTemplate();

    // 너무 긴 텍스트는 잘라서 보냄 (토큰 폭주 방지)
    private static final int MAX_TEXT_LENGTH = 8000;

    /**
     * PDF 바이너리 데이터를 받아서 텍스트를 추출한 뒤
     * 그 텍스트를 기반으로 Gemini에게 문제 생성을 요청
     */
    public String generateQuestionsFromPdf(byte[] pdfBytes, String originalName) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extracted = stripper.getText(document);

            if (extracted == null || extracted.isBlank()) {
                return "⚠ PDF에서 텍스트를 추출하지 못했습니다.";
            }

            if (extracted.length() > MAX_TEXT_LENGTH) {
                extracted = extracted.substring(0, MAX_TEXT_LENGTH);
            }

            return generateQuestionsFromText(originalName, extracted);

        } catch (IOException e) {
            return "⚠ PDF 처리 중 오류: " + e.getMessage();
        }
    }

    /**
     * 기존에 사용하던 텍스트 기반 메서드는 그대로 두되,
     * 내부적으로 파일 이름을 모르는 버전으로 연결
     */
    public String generateQuestionsFromText(String text) {
        return generateQuestionsFromText(null, text);
    }

    /**
     * (실제 호출) 텍스트를 기반으로 Gemini에 문제 생성을 요청
     * originalName이 있으면 프롬프트에 함께 넣어줌
     */
    private String generateQuestionsFromText(String originalName, String text) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        String titlePart = (originalName != null && !originalName.isBlank())
                ? "파일 이름: " + originalName + "\n\n"
                : "";

        String prompt = """
                너는 대학 강의자료나 교재 PDF를 기반으로 학습용 문제를 만들어주는 도우미야.

                아래 내용을 기반으로:
                - 객관식 3문제
                - 주관식(단답형/서술형) 2문제

                총 5문제를 만들어줘.

                각 문제는 반드시 아래 형식을 지켜줘:

                [문제 번호]. 문제 내용
                (보기) 1) ..., 2) ..., 3) ..., 4) ...   <-- 객관식일 때만
                [정답] 숫자 또는 텍스트
                [해설] 한두 문장으로 간단하게 이유 설명

                주관식 문제에는 (보기)를 넣지 말고, 정답과 해설만 작성해줘.

                """ + titlePart + "텍스트:\n" + text;

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
            if (data == null) {
                return "⚠ Gemini 응답이 비어 있습니다.";
            }

            List candidates = (List) data.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return "⚠ Gemini에서 후보 응답을 받지 못했습니다.";
            }

            Map first = (Map) candidates.get(0);
            Map content = (Map) first.get("content");
            if (content == null) {
                return "⚠ Gemini 응답 형식이 예상과 다릅니다.(content 없음)";
            }

            List parts = (List) content.get("parts");
            if (parts == null || parts.isEmpty()) {
                return "⚠ Gemini 응답 형식이 예상과 다릅니다.(parts 없음)";
            }

            Map firstPart = (Map) parts.get(0);
            Object textObj = firstPart.get("text");
            if (textObj == null) {
                return "⚠ Gemini 응답에 text가 없습니다.";
            }

            return textObj.toString();

        } catch (Exception e) {
            return "⚠ Gemini 오류: " + e.getMessage();
        }
    }
}
