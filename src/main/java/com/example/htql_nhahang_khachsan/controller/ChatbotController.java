package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.ChatbotRequest;
import com.example.htql_nhahang_khachsan.dto.ChatbotResponse;
import com.example.htql_nhahang_khachsan.repository.ChatbotBookingDraftRepository;
import com.example.htql_nhahang_khachsan.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import com.example.htql_nhahang_khachsan.entity.ChatbotBookingDraftEntity;
import com.example.htql_nhahang_khachsan.enums.BookingDraftStep;
import java.time.LocalDateTime;
import java.util.*;


@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;
    private final ChatbotBookingDraftRepository draftRepository;

    /**
     * ✅ Endpoint chính - Nhận tin nhắn từ user, trả về câu trả lời
     * Luôn trả về JSON có cấu trúc hoặc JSON chứa văn bản thuần túy.
     */
//    @PostMapping
//    public Mono<ResponseEntity<String>> chat(@Valid @RequestBody ChatbotRequest request) {
//        log.info("📩 Chatbot received message: {}", request.getMessage());
//        log.info("... with history: {}", request.getHistory() != null && !request.getHistory().isEmpty());
//
//        return chatbotService.getReply(request.getMessage(), request.getHistory())
//                .map(reply -> {
//                    log.info("✅ Chatbot replied: {}", reply);
//                    // Tiếng Việt: Luôn trả về JSON từ service với content type là application/json
//                    return ResponseEntity.ok()
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .body(reply);
//                })
//                .onErrorResume(e -> {
//                    log.error("❌ Chatbot error: {}", e.getMessage(), e);
//                    // Tiếng Việt: Trả về lỗi dưới dạng JSON để frontend có thể xử lý nhất quán
//                    String errorJson = "{\"reply\": \"Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau.\"}";
//                    return Mono.just(
//                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .body(errorJson)
//                    );
//                });
//    }

    @PostMapping
    public Mono<ResponseEntity<String>> chat(@Valid @RequestBody ChatbotRequest request) {
        // ✅ Log để debug
        log.info("📩 Chatbot received message: {}", request.getMessage());
        log.info("📊 History length: {}", request.getHistory() != null ? request.getHistory().length() : 0);

        // ✅ THÊM: Truncate history nếu quá dài để tránh lỗi validation
        if (request.getHistory() != null && request.getHistory().length() > 4000) {
            log.warn("⚠️ History too long ({}), truncating to 4000 chars", request.getHistory().length());
            request.setHistory(request.getHistory().substring(
                    Math.max(0, request.getHistory().length() - 4000)
            ));
        }

        return chatbotService.getReply(request.getMessage(), request.getHistory())
                .map(reply -> {
                    log.info("✅ Chatbot replied: {}", reply.length() > 100 ? reply.substring(0, 100) + "..." : reply);
                    // ✅ Luôn trả về JSON với content type đúng
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(reply);
                })
                .onErrorResume(e -> {
                    log.error("❌ Chatbot error: {}", e.getMessage(), e);
                    // ✅ Trả về lỗi dạng JSON để frontend xử lý nhất quán
                    String errorJson = "{\"reply\": \"❌ Xin lỗi, đã có lỗi xảy ra: " + e.getMessage().replace("\"", "'") + "\"}";
                    return Mono.just(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(errorJson)
                    );
                });
    }


    /**
     * ✅ Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot service is running");
    }


    /**
     * ✅ THÊM: Check draft codes nào còn valid
     * Frontend gọi để filter messages trong localStorage
     */
    @PostMapping("/check-drafts")
    public ResponseEntity<Map<String, Object>> checkDrafts(
            @RequestBody Map<String, List<String>> request) {

        log.info("📋 Checking draft status for {} codes", request.get("draftCodes").size());

        List<String> draftCodes = request.get("draftCodes");
        List<String> validDrafts = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        for (String code : draftCodes) {
            Optional<ChatbotBookingDraftEntity> draftOpt = draftRepository.findByDraftCode(code);

            if (draftOpt.isPresent()) {
                ChatbotBookingDraftEntity draft = draftOpt.get();

                // Check chưa expired và chưa hoàn tất
                if (draft.getExpiresAt().isAfter(now) &&
                        draft.getCurrentStep() != BookingDraftStep.READY_TO_PAY) {
                    validDrafts.add(code);
                    log.info("✅ Draft {} is valid", code);
                } else {
                    log.info("❌ Draft {} expired or completed", code);
                    // Xóa draft đã hết hạn/hoàn tất
                    try {
                        draftRepository.delete(draft);
                    } catch (Exception e) {
                        log.warn("Failed to delete draft {}", code);
                    }
                }
            } else {
                log.info("❌ Draft {} not found", code);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("validDrafts", validDrafts);
        response.put("total", draftCodes.size());
        response.put("valid", validDrafts.size());

        return ResponseEntity.ok(response);
    }

    /**
     * ✅ THÊM: Xóa draft code (khi user cancel)
     */
    @DeleteMapping("/draft/{draftCode}")
    public ResponseEntity<Map<String, String>> cancelDraft(@PathVariable String draftCode) {
        log.info("🗑️ Cancelling draft: {}", draftCode);

        try {
            Optional<ChatbotBookingDraftEntity> draftOpt = draftRepository.findByDraftCode(draftCode);

            if (draftOpt.isPresent()) {
                draftRepository.delete(draftOpt.get());

                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Draft cancelled");

                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("status", "not_found");
                response.put("message", "Draft not found");

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error cancelling draft: ", e);

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * ✅ THÊM: Cleanup expired drafts (có thể gọi định kỳ)
     */
    @PostMapping("/cleanup-drafts")
    public ResponseEntity<Map<String, Object>> cleanupExpiredDrafts() {
        log.info("🧹 Running draft cleanup...");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<ChatbotBookingDraftEntity> expiredDrafts =
                    draftRepository.findByExpiresAtBefore(now);

            int count = expiredDrafts.size();

            if (count > 0) {
                draftRepository.deleteAll(expiredDrafts);
                log.info("✅ Cleaned up {} expired drafts", count);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("cleaned", count);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cleaning up drafts: ", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}