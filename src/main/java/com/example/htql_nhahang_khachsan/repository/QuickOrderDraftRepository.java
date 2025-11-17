package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.QuickOrderDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuickOrderDraftRepository extends JpaRepository<QuickOrderDraftEntity, Long> {

    Optional<QuickOrderDraftEntity> findByDraftCode(String draftCode);

    List<QuickOrderDraftEntity> findBySessionId(String sessionId);

    List<QuickOrderDraftEntity> findByExpiresAtBefore(LocalDateTime dateTime);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
