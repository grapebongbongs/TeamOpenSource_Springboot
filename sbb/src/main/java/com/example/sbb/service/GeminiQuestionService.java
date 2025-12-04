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
import java.util.stream.Collectors;

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
                (보기)
                1) ...
                2) ...
                3) ...
                4) ...   <-- 객관식일 때만, 보기 4개는 항상 채워줘
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
                                                    List<String> originalNames,
                                                    String stylePrompt) {

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

        String basePrompt = """
                너는 대학 강의자료나 교재 PDF를 기반으로 학습용 문제를 만들어주는 도우미야.

                아래에 첨부된 여러 개의 PDF 문서를 모두 읽고,
                전체 내용을 종합해서 아래 조건을 만족하는 문제를 만들어줘.

                - 총 10문제
                - 객관식 6문제
                - 주관식(단답형/서술형) 4문제

                각 문제는 반드시 아래 형식을 지켜줘:

                [번호]. 문제 내용
                (보기)
                1) ...
                2) ...
                3) ...
                4) ...   <-- 객관식일 때만, 보기 4개는 항상 채워줘
                [정답] 숫자 또는 텍스트
                [해설] 한두 문장으로 간단하게 이유 설명

                주관식 문제에는 (보기)를 넣지 말고, 정답과 해설만 작성해줘.

                """;
        StringBuilder promptBuilder = new StringBuilder(basePrompt).append("\n").append(titleBuilder);
        if (stylePrompt != null && !stylePrompt.isBlank()) {
            promptBuilder
                    .append("\n사용자가 원하는 문제 스타일/중점 사항:\n")
                    .append(stylePrompt)
                    .append("\n가능한 한 위 요구사항을 반영해서 문제를 만들어줘.\n");
        }

        // 1) parts 리스트 구성: 첫 번째 part는 텍스트 prompt
        List<Map<String, Object>> parts = new ArrayList<>();
        parts.add(Map.of("text", promptBuilder.toString()));

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

    public String generateQuestionsFromTexts(List<String> texts,
                                             List<String> originalNames,
                                             String stylePrompt) {
        if (texts == null || texts.isEmpty()) {
            return "⚠ 전달된 텍스트가 없습니다.";
        }

        StringBuilder namePart = new StringBuilder();
        if (originalNames != null && !originalNames.isEmpty()) {
            namePart.append("다음 자료를 기반으로 문제를 만들어줘:\n");
            for (String name : originalNames) {
                if (name != null && !name.isBlank()) {
                    namePart.append("- ").append(name).append("\n");
                }
            }
            namePart.append("\n");
        }

        StringBuilder textBundle = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            String label = (originalNames != null && i < originalNames.size())
                    ? originalNames.get(i)
                    : "자료 " + (i + 1);
            textBundle.append("### ").append(label).append("\n")
                    .append(texts.get(i)).append("\n\n");
        }

        StringBuilder prompt = new StringBuilder("""
                너는 대학 강의자료나 교재 텍스트를 기반으로 학습용 문제를 만들어주는 도우미야.

                모든 텍스트를 종합하여 총 10문제를 만들어줘.
                - 객관식 6문제 (보기는 4개, 번호를 꼭 붙여줘)
                - 주관식(단답형/서술형) 4문제

                각 문제는 아래 형식을 꼭 지켜줘:
                [번호]. 문제 내용
                (보기) 1. ... 2. ... 3. ... 4. ...   <-- 객관식일 때만
                [정답] 숫자 또는 텍스트
                [해설] 한두 문장으로 간단하게 이유 설명

                주관식 문제에는 (보기)를 넣지 말아줘.

                """);

        if (stylePrompt != null && !stylePrompt.isBlank()) {
            prompt.append("사용자 요청 스타일:\n").append(stylePrompt).append("\n\n");
        }
        prompt.append(namePart);
        prompt.append("아래 텍스트 전체를 분석해서 문제를 만들어줘:\n");
        prompt.append(textBundle);

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", prompt.toString())
                                )
                        )
                )
        );

        return callGemini(body);
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
                (보기)
                1) ...
                2) ...
                3) ...
                4) ...   <-- 객관식일 때만, 보기 4개는 항상 채워줘
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

    /**
     * Gemini 호출 결과가 실패/에러로 판단되는지 여부
     */
    public boolean isFailure(String result) {
        if (result == null) return true;
        String trimmed = result.trim();
        if (trimmed.isEmpty()) return true;
        return trimmed.startsWith("⚠") || trimmed.toLowerCase().contains("error") || trimmed.toLowerCase().contains("오류");
    }

    /**
     * 네트워크가 막혀 있거나 Gemini 응답이 비었을 때
     * 추출 텍스트만으로 간단한 10문제(객관식 6, 주관식 4)를 생성한다.
     */
    public String fallbackQuestionsFromTexts(List<String> texts,
                                             List<String> originalNames,
                                             String stylePrompt) {
        List<String> sentences = new ArrayList<>();
        for (String text : texts) {
            if (text == null) continue;
            String[] parts = text.split("(?<=[.!?\\n])");
            for (String p : parts) {
                String s = p.trim();
                if (s.length() >= 10) {
                    sentences.add(s);
                }
            }
        }
        if (sentences.isEmpty()) {
            sentences.add("자료의 핵심 개념을 정리해보세요.");
        }

        List<String> names = (originalNames == null) ? new ArrayList<>() : originalNames;
        List<String> questions = new ArrayList<>();
        String styleLine = (stylePrompt != null && !stylePrompt.isBlank())
                ? "요청된 스타일: " + stylePrompt + "\n"
                : "";

        // 객관식 6문제
        for (int i = 0; i < 6; i++) {
            String base = sentences.get(i % sentences.size());
            List<String> tokens = extractKeywords(base, 4);
            while (tokens.size() < 4) {
                tokens.add("추가 선택지 " + (tokens.size() + 1));
            }
            String question = "[%d]. 다음 설명과 가장 잘 맞는 핵심어는 무엇인가?\n%s\n(보기) 1. %s 2. %s 3. %s 4. %s\n[정답] 1\n[해설] 텍스트에서 가장 먼저 추출된 핵심어입니다."
                    .formatted(i + 1, base, tokens.get(0), tokens.get(1), tokens.get(2), tokens.get(3));
            questions.add(question);
        }

        // 주관식 4문제
        for (int i = 0; i < 4; i++) {
            int number = 7 + i;
            String base = sentences.get((i + 6) % sentences.size());
            String label = (i < names.size()) ? names.get(i) : "자료";
            String question = "[%d]. %s의 주요 개념을 한 줄로 요약하세요.\n문장: %s\n[정답] 핵심 개념을 요약해서 입력\n[해설] 문장을 한 줄로 요약하면 됩니다."
                    .formatted(number, label, base);
            questions.add(question);
        }

        return """
                %s
                %s
                """.formatted(styleLine, String.join("\n\n", questions)).trim();
    }

    private List<String> extractKeywords(String text, int limit) {
        if (text == null || text.isBlank()) return new ArrayList<>();
        String[] words = text.replaceAll("[^A-Za-z0-9가-힣\\s]", " ")
                .toLowerCase()
                .split("\\s+");
        List<String> filtered = new ArrayList<>();
        for (String w : words) {
            if (w.length() >= 2 && !filtered.contains(w)) {
                filtered.add(w);
            }
        }
        return filtered.stream().limit(limit).collect(Collectors.toList());
    }
}
