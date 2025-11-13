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
public class DocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;

    private LocalDateTime uploadedAt;

    @ManyToOne
    private SiteUser user;

    // ✅ PDF에서 뽑은 텍스트를 넣어 둘 컬럼
    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;
}
