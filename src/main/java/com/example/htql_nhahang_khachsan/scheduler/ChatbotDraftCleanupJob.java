package com.example.htql_nhahang_khachsan.scheduler;

import com.example.htql_nhahang_khachsan.entity.ChatbotBookingDraftEntity;
import com.example.htql_nhahang_khachsan.repository.ChatbotBookingDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class ChatbotDraftCleanupJob {

    @Autowired
    private ChatbotBookingDraftRepository draftRepository;

    // Chạy mỗi 1 giờ
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredDrafts() {
        LocalDateTime now = LocalDateTime.now();
        List<ChatbotBookingDraftEntity> expiredDrafts =
                draftRepository.findAll().stream()
                        .filter(d -> d.getExpiresAt().isBefore(now))
                        .toList();

        if (!expiredDrafts.isEmpty()) {
            draftRepository.deleteAll(expiredDrafts);
            System.out.println("✅ Cleaned up " + expiredDrafts.size() + " expired booking drafts");
        }
    }
}