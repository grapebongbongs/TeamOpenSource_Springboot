package com.example.sbb.repository;

import com.example.sbb.domain.group.GroupInvite;
import com.example.sbb.domain.group.StudyGroup;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {
    List<GroupInvite> findByToUserAndStatus(SiteUser user, GroupInvite.Status status);
    List<GroupInvite> findByFromUserAndStatus(SiteUser user, GroupInvite.Status status);
    Optional<GroupInvite> findByIdAndToUser(Long id, SiteUser user);
    boolean existsByGroupAndToUserAndStatus(StudyGroup group, SiteUser toUser, GroupInvite.Status status);
}
