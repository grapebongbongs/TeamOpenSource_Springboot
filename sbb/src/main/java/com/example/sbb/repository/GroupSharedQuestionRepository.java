package com.example.sbb.repository;

import com.example.sbb.domain.group.GroupSharedQuestion;
import com.example.sbb.domain.group.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupSharedQuestionRepository extends JpaRepository<GroupSharedQuestion, Long> {
    List<GroupSharedQuestion> findByGroupOrderBySharedAtDesc(StudyGroup group);
    boolean existsByGroup_IdAndQuestion_Id(Long groupId, Long questionId);
    void deleteAllByQuestion_IdIn(List<Long> questionIds);
    void deleteAllByGroup(StudyGroup group);
}
