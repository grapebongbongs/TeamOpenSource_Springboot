package com.example.sbb.repository;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProblemRepository extends JpaRepository<Problem, Long> {
    void deleteAllByDocumentFile(DocumentFile documentFile);
}
