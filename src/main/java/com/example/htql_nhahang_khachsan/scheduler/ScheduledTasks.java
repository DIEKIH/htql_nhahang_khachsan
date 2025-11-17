
package com.example.htql_nhahang_khachsan.scheduler;

import com.example.htql_nhahang_khachsan.entity.ChatbotBookingDraftEntity;
import com.example.htql_nhahang_khachsan.repository.ChatbotBookingDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final ChatbotBookingDraftRepository draftRepository;

//    /**
//     * ✅ Cleanup expired drafts mỗi 30 phút
//     * Cron: 0 */30 * * * * = mỗi 30 phút
//     */
    @Scheduled(cron = "0 */30 * * * *")
    @Transactional
    public void cleanupExpiredDrafts() {
        log.info("🧹 [SCHEDULED] Running draft cleanup...");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<ChatbotBookingDraftEntity> expiredDrafts =
                    draftRepository.findByExpiresAtBefore(now);

            if (!expiredDrafts.isEmpty()) {
                draftRepository.deleteAll(expiredDrafts);
                log.info("✅ [SCHEDULED] Cleaned up {} expired drafts", expiredDrafts.size());
            } else {
                log.info("ℹ️ [SCHEDULED] No expired drafts found");
            }

        } catch (Exception e) {
            log.error("❌ [SCHEDULED] Error cleaning up drafts: ", e);
        }
    }

    /**
     * ✅ Cleanup drafts quá cũ (>24h) mỗi ngày lúc 2h sáng
     * Cron: 0 0 2 * * * = 2:00 AM mỗi ngày
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldDrafts() {
        log.info("🧹 [SCHEDULED] Running old drafts cleanup...");

        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            List<ChatbotBookingDraftEntity> oldDrafts =
                    draftRepository.findByCreatedAtBefore(cutoff);

            if (!oldDrafts.isEmpty()) {
                draftRepository.deleteAll(oldDrafts);
                log.info("✅ [SCHEDULED] Cleaned up {} old drafts (>24h)", oldDrafts.size());
            } else {
                log.info("ℹ️ [SCHEDULED] No old drafts found");
            }

        } catch (Exception e) {
            log.error("❌ [SCHEDULED] Error cleaning up old drafts: ", e);
        }
    }
}