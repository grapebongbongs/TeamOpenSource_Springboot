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

    private String originalFilename;   // 업로드된 파일명
    private String storedFilename;     // 서버에 저장된 파일명
    private String filePath;           // 저장 경로
    private long fileSize;             // 파일 크기

    private LocalDateTime uploadedAt;  // 업로드 시각

    @ManyToOne
    @JoinColumn(name = "user_id")
    private SiteUser user;             // 업로더

    @Column(columnDefinition = "LONGTEXT")
    private String contentText;        // PDF에서 추출한 텍스트
}
