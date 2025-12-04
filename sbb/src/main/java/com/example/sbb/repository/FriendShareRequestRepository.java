package com.example.sbb.repository;

import com.example.sbb.domain.user.FriendShareRequest;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendShareRequestRepository extends JpaRepository<FriendShareRequest, Long> {
    List<FriendShareRequest> findByToUserAndStatus(SiteUser toUser, FriendShareRequest.Status status);
    List<FriendShareRequest> findByFromUserAndStatus(SiteUser fromUser, FriendShareRequest.Status status);
    Optional<FriendShareRequest> findByIdAndToUser(Long id, SiteUser toUser);
    void deleteAllByQuestion_IdIn(List<Long> questionIds);
}
