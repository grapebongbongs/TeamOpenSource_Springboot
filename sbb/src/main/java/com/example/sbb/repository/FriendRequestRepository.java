package com.example.sbb.repository;

import com.example.sbb.domain.user.FriendRequest;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    boolean existsByFromUserAndToUserAndStatus(SiteUser from, SiteUser to, FriendRequest.Status status);
    List<FriendRequest> findByToUserAndStatus(SiteUser toUser, FriendRequest.Status status);
    List<FriendRequest> findByFromUserAndStatus(SiteUser fromUser, FriendRequest.Status status);
    Optional<FriendRequest> findByIdAndToUser(Long id, SiteUser toUser);
}
