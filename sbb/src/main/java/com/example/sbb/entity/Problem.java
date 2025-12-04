package com.example.sbb.entity;

import com.example.sbb.domain.document.DocumentFile;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "problem")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 PDF(DocumentFile) 기반 문제인지
    @ManyToOne
    @JoinColumn(name = "document_file_id")
    private DocumentFile documentFile;

    // Gemini가 생성한 문제 텍스트
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String problemText;

    // 문제 생성 시각
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Problem() {}

    // ===== Getter / Setter =====

    public Long getId() {
        return id;
    }

    public DocumentFile getDocumentFile() {
        return documentFile;
    }

    public void setDocumentFile(DocumentFile documentFile) {
        this.documentFile = documentFile;
    }

    public String getProblemText() {
        return problemText;
    }

    public void setProblemText(String problemText) {
        this.problemText = problemText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
