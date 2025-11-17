package com.example.htql_nhahang_khachsan.repository;


import com.example.htql_nhahang_khachsan.entity.ChatbotBookingDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotBookingDraftRepository extends JpaRepository<ChatbotBookingDraftEntity, Long> {
    Optional<ChatbotBookingDraftEntity> findByDraftCode(String draftCode);
    Optional<ChatbotBookingDraftEntity> findBySessionId(String sessionId);

    @Query("SELECT d FROM ChatbotBookingDraftEntity d WHERE d.expiresAt > :now")
    List<ChatbotBookingDraftEntity> findActiveBookingDrafts(@Param("now") LocalDateTime now);


    // ✅ THÊM: Tìm draft expired
    List<ChatbotBookingDraftEntity> findByExpiresAtBefore(LocalDateTime dateTime);

    // ✅ THÊM: Tìm draft theo user (để cleanup khi user logout)
    List<ChatbotBookingDraftEntity> findBySessionIdStartingWith(String prefix);

    // ✅ THÊM: Tìm draft tạo trước thời điểm X
    List<ChatbotBookingDraftEntity> findByCreatedAtBefore(LocalDateTime dateTime);
}
