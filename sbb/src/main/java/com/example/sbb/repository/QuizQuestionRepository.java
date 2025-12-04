package com.example.sbb.repository;

import com.example.sbb.domain.document.DocumentFile;
import com.example.sbb.domain.Folder;
import com.example.sbb.domain.quiz.QuizQuestion;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    // 아직 안 푼 문제
    List<QuizQuestion> findByUserAndSolvedFalseOrderByCreatedAtAsc(SiteUser user);
    List<QuizQuestion> findByUserAndFolderAndSolvedFalseOrderByCreatedAtAsc(SiteUser user, Folder folder);

    // 맞은 문제
    List<QuizQuestion> findByUserAndSolvedTrueAndCorrectTrueOrderByCreatedAtAsc(SiteUser user);

    // 틀린 문제
    List<QuizQuestion> findByUserAndSolvedTrueAndCorrectFalseOrderByCreatedAtAsc(SiteUser user);

    List<QuizQuestion> findByUserAndSolvedTrueOrderByCreatedAtAsc(SiteUser user);

    Page<QuizQuestion> findByUserOrderByCreatedAtAsc(SiteUser user, Pageable pageable);
    Page<QuizQuestion> findByUserAndFolderOrderByCreatedAtAsc(SiteUser user, Folder folder, Pageable pageable);
    List<QuizQuestion> findByUserOrderByCreatedAtAsc(SiteUser user);
    List<QuizQuestion> findByUserAndFolderOrderByCreatedAtAsc(SiteUser user, Folder folder);

    void deleteAllByDocument(DocumentFile documentFile);
    void deleteAllByFolder(Folder folder);
    List<QuizQuestion> findByFolder(Folder folder);
    List<QuizQuestion> findByDocument(DocumentFile documentFile);
}
