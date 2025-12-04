package com.example.sbb.repository;

import com.example.sbb.domain.group.GroupMember;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByUser(SiteUser user);
    List<GroupMember> findByGroup(StudyGroup group);
    Optional<GroupMember> findByGroupAndUser(StudyGroup group, SiteUser user);
    List<GroupMember> findByGroupOrderByJoinedAtAsc(StudyGroup group);
    void deleteByGroupAndUser(StudyGroup group, SiteUser user);
}
