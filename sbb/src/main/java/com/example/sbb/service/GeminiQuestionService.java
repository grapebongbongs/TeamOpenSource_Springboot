package com.example.sbb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class GeminiQuestionService {

    @Value("${gemini.api.key}")
    private String apiKey;

    // 멀티모달(이미지/PDF) 지원 모델 사용 (예: gemini-1.5-flash, gemini-1.5-pro 등)
    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;

    // 간단히 내부에서 생성 (원하면 @Bean으로 주입해도 됨)
    private final RestTemplate restTemplate = new RestTemplate();

    // =========================
    // 1) 단일 PDF → 문제 생성
    // =========================
    public String generateQuestionsFromPdf(byte[] pdfBytes, String originalName) {

        if (pdfBytes == null || pdfBytes.length == 0) {
            return "⚠ 전달된 PDF 데이터가 비어 있습니다.";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        String titlePart = (originalName != null && !originalName.isBlank())
                ? "파일 이름: " + originalName + "\n\n"
                : "";

        String prompt = """
                너는 대학 강의자료나 교재 PDF를 기반으로 학습용 문제를 만들어주는 도우미야.

                아래 PDF 문서를 읽고:
                - 객관식 3문제
                - 주관식(단답형/서술형) 2문제

                총 5문제를 만들어줘.

                각 문제는 반드시 아래 형식을 지켜줘:

                [번호]. 문제 내용
                (보기) 1) ..., 2) ..., 3) ..., 4) ...   <-- 객관식일 때만
                [정답] 숫자 또는 텍스트
                [해설] 한두 문장으로 간단하게 이유 설명

                주관식 문제에는 (보기)를 넣지 말고, 정답과 해설만 작성해줘.

                """ + titlePart + "PDF는 아래에 첨부되어 있어.\n";

        // PDF를 base64로 인코딩
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        // parts 구성: prompt + inlineData(pdf)
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));
        parts.add(
                Map.of(
                        "inlineData", Map.of(
                                "mimeType", "application/pdf",
                                "data", base64Pdf
                        )
                )
        );

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", parts
                        )
                )
        );

        return callGemini(body);
    }

    // =========================
    // 2) 여러 PDF → 한 번에 문제 생성
    // =========================
    public String generateQuestionsFromMultiplePdfs(List<byte[]> pdfBytesList,
                                                    List<String> originalNames) {

        if (pdfBytesList == null || pdfBytesList.isEmpty()) {
            return "⚠ 전달된 PDF가 없습니다.";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        StringBuilder titleBuilder = new StringBuilder();
        if (originalNames != null && !originalNames.isEmpty()) {
            titleBuilder.append("다음 ").append(originalNames.size()).append("개의 문서를 기반으로 문제를 만들어줘.\n");
            for (int i = 0; i < originalNames.size(); i++) {
                titleBuilder.append("- ").append(originalNames.get(i)).append("\n");
            }
            titleBuilder.append("\n");
        }

        String prompt = """
                너는 대학 강의자료나 교재 PDF를 기반으로 학습용 문제를 만들어주는 도우미야.

                아래에 첨부된 여러 개의 PDF 문서를 모두 읽고,
                전체 내용을 종합해서 아래 조건을 만족하는 문제를 만들어줘.

                - 총 10문제
                - 객관식 6문제
                - 주관식(단답형/서술형) 4문제

                각 문제는 반드시 아래 형식을 지켜줘:

                [번호]. 문제 내용
                (보기) 1) ..., 2) ..., 3) ..., 4) ...   <-- 객관식일 때만
                [정답] 숫자 또는 텍스트
                [해설] 한두 문장으로 간단하게 이유 설명

                주관식 문제에는 (보기)를 넣지 말고, 정답과 해설만 작성해줘.

                """ + titleBuilder;

        // 1) parts 리스트 구성: 첫 번째 part는 텍스트 prompt
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", prompt));

        // 2) 각 PDF를 inlineData로 추가
        for (byte[] pdfBytes : pdfBytesList) {
            if (pdfBytes == null || pdfBytes.length == 0) continue;

            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
            parts.add(
                    Map.of(
                            "inlineData", Map.of(
                                    "mimeType", "application/pdf",
                                    "data", base64Pdf
                            )
                    )
            );
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", parts
                        )
                )
        );

        return callGemini(body);
    }

    // =========================
    // 3) (옵션) 텍스트만 직접 넣어서 문제 생성
    // =========================
    public String generateQuestionsFromText(String text) {
        return generateQuestionsFromText(null, text);
    }

    private String generateQuestionsFromText(String originalName, String text) {

        if (text == null || text.isBlank()) {
            return "⚠ 전달된 텍스트가 비어 있습니다.";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

        String titlePart = (originalName != null && !originalName.isBlank())
                ? "파일 이름: " + originalName + "\n\n"
                : "";

        String prompt = """
                너는 대학 강의자료나 교재 텍스트를 기반으로 학습용 문제를 만들어주는 도우미야.

                아래 내용을 기반으로:
                - 객관식 3문제
                - 주관식(단답형/서술형) 2문제

                총 5문제를 만들어줘.

                각 문제는 반드시 아래 형식을 지켜줘:

                [번호]. 문제 내용
                (보기) 1) ..., 2) ..., 3) ..., 4) ...   <-- 객관식일 때만
                [정답] 숫자 또는 텍스트
                [해설] 한두 문장으로 간단하게 이유 설명

                주관식 문제에는 (보기)를 넣지 말고, 정답과 해설만 작성해줘.

                """ + titlePart + "텍스트:\n" + text;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        return callGemini(body);
    }

    // =========================
    // 4) 공통 Gemini 호출 로직
    // =========================
    @SuppressWarnings("rawtypes")
    private String callGemini(Map<String, Object> body) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + modelName + ":generateContent?key=" + apiKey;

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

            List partsResp = (List) content.get("parts");
            if (partsResp == null || partsResp.isEmpty()) {
                return "⚠ Gemini 응답 형식이 예상과 다릅니다.(parts 없음)";
            }

            Map firstPart = (Map) partsResp.get(0);
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
