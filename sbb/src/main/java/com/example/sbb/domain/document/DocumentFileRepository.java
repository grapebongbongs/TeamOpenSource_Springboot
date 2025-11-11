package com.example.sbb.domain.document;

import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentFileRepository extends JpaRepository<DocumentFile, Long> {
    List<DocumentFile> findByUser(SiteUser user);
}
