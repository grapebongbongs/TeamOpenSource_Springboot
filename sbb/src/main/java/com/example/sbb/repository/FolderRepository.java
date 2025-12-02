package com.example.sbb.repository;

import com.example.sbb.domain.Folder;
import com.example.sbb.domain.user.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    List<Folder> findByUserOrderByCreatedAtAsc(SiteUser user);
    Optional<Folder> findByIdAndUser(Long id, SiteUser user);
}
