package com.example.sbb.repository;

import com.example.sbb.domain.user.FriendShareComment;
import com.example.sbb.domain.user.FriendShareRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendShareCommentRepository extends JpaRepository<FriendShareComment, Long> {
    List<FriendShareComment> findByShareRequestOrderByCreatedAtAsc(FriendShareRequest shareRequest);
}
