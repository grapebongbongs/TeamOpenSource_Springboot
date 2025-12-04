package com.example.sbb.repository;

import com.example.sbb.domain.user.Friend;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    boolean existsByFromAndTo(SiteUser from, SiteUser to);
    List<Friend> findByFrom(SiteUser from);
    Optional<Friend> findByFromAndTo(SiteUser from, SiteUser to);
    void deleteByFromAndTo(SiteUser from, SiteUser to);
}
