package com.example.sbb.repository;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {

    // 로그인한 유저 기준으로 자기 파일 목록 가져오기
    List<DocumentFile> findByUser(SiteUser user);
    List<DocumentFile> findByUserAndFolder(SiteUser user, Folder folder);
    List<DocumentFile> findByFolder(Folder folder);
}
