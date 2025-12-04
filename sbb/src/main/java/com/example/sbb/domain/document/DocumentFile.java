package com.example.sbb.domain.document;

import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_file")
public class DocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 업로드된 원본 파일명
    @Column(nullable = false)
    private String originalFilename;

    // 서버에 저장된 파일명 (ex: 1731400000000_문서.pdf)
    @Column(nullable = false)
    private String storedFilename;

    // 파일 크기 (byte)
    @Column(nullable = false)
    private Long fileSize;

    // 업로드 시간
    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    // 🔗 업로드한 사용자 (외래키)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private SiteUser user;

    // 과목/폴더
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    // ✅ PDF에서 추출한 텍스트를 저장할 필드 (스케줄러/Gemini용)
    @Column(columnDefinition = "LONGTEXT")
    private String extractedText;

    public DocumentFile() {
    }

    public DocumentFile(String originalFilename,
                        String storedFilename,
                        Long fileSize,
                        SiteUser user) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileSize = fileSize;
        this.user = user;
        this.uploadedAt = LocalDateTime.now();
    }

    // ====== getter / setter ======

    public Long getId() {
        return id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public SiteUser getUser() {
        return user;
    }

    public void setUser(SiteUser user) {
        this.user = user;
    }

    // ✅ 새로 추가한 필드용 getter/setter
    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}
