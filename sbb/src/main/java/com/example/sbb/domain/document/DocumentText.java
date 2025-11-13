package com.example.sbb.domain.document;

import com.example.sbb.domain.user.SiteUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "document_text")
public class DocumentText {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;  // 업로드된 PDF 파일명

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt; // 업로드 시각

    @ManyToOne
    @JoinColumn(name = "user_id")
    private SiteUser user; // 업로더 (외래키)

    @Column(name = "content_text", columnDefinition = "LONGTEXT")
    private String contentText; // 추출된 PDF 텍스트
}
