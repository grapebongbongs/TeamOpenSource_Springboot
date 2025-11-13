package com.example.sbb.repository;

import com.example.sbb.domain.document.DocumentText;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentTextRepository extends JpaRepository<DocumentText, Long> {
    List<DocumentText> findByUser(SiteUser user);
}
