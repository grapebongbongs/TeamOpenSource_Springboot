package com.example.sbb.service;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.entity.Problem;
import com.example.sbb.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    /**
     * PDF 한 개(DocumentFile)와 그 PDF를 기반으로 생성된 문제 텍스트(text)를
     * Problem 엔티티로 만들어 DB에 저장하는 메서드.
     */
    public void saveProblem(DocumentFile documentFile, String text) {

        // 내용이 없으면 저장 안 함
        if (text == null || text.isBlank()) {
            return;
        }

        // 🔹 빌더 대신 기본 생성자 + setter 사용
        Problem p = new Problem();
        p.setDocumentFile(documentFile);          // 어떤 PDF에서 나온 문제인지 연결
        p.setProblemText(text);                  // Gemini가 생성한 문제 텍스트
        p.setCreatedAt(LocalDateTime.now());     // 생성 시각

        problemRepository.save(p);
    }
}
