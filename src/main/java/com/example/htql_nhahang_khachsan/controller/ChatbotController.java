package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.ChatbotRequest;
import com.example.htql_nhahang_khachsan.dto.ChatbotResponse;
import com.example.htql_nhahang_khachsan.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * ✅ Endpoint chính - Nhận tin nhắn từ user, trả về câu trả lời
     */
    @PostMapping
    public Mono<ResponseEntity<ChatbotResponse>> chat(@Valid @RequestBody ChatbotRequest request) {
        log.info("📩 Chatbot received message: {}", request.getMessage());

        return chatbotService.getReply(request.getMessage())
                .map(reply -> {
                    log.info("✅ Chatbot replied: {}", reply);
                    return ResponseEntity.ok(new ChatbotResponse(reply));
                })
                .onErrorResume(e -> {
                    log.error("❌ Chatbot error: {}", e.getMessage(), e);
                    return Mono.just(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(new ChatbotResponse("Xin lỗi, tôi đang gặp sự cố. Vui lòng thử lại sau."))
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
}