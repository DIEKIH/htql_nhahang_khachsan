package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.repository.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final WebClient webClient;
    private final String apiKey;
    private final BranchRepository branchRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoomRepository roomRepository;

    private final String modelName;

    public ChatbotService(WebClient.Builder webClientBuilder,
                          @Value("${gemini.api.key}") String apiKey,
                          @Value("${gemini.model:gemini-1.5-flash-latest}") String modelName,
                          BranchRepository branchRepository,
                          RoomTypeRepository roomTypeRepository,
                          MenuItemRepository menuItemRepository,
                          RoomRepository roomRepository) {
        // ✅ Base URL đúng cho Google AI Studio
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.branchRepository = branchRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.roomRepository = roomRepository;

        System.out.println("✅ Chatbot initialized with model: " + modelName);
    }

    /**
     * ✅ Main method - Gọi API Gemini với context đầy đủ
     */
    public Mono<String> getReply(String userMessage) {
        String systemContext = buildSystemContext();
        String fullPrompt = buildPrompt(systemContext, userMessage);
        JSONObject requestBody = createGeminiRequest(fullPrompt);

        // ✅ Endpoint với model name từ config
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/models/" + modelName + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .onErrorResume(e -> {
                    System.err.println("❌ Gemini API Error: " + e.getMessage());
                    e.printStackTrace();
                    return Mono.just("Xin lỗi, tôi đang gặp sự cố kết nối. Vui lòng thử lại sau. Chi tiết: " + e.getMessage());
                });
    }

    /**
     * ✅ Xây dựng context về hệ thống
     */
    private String buildSystemContext() {
        StringBuilder context = new StringBuilder();

        try {
            // 1. Thông tin chi nhánh
            List<BranchEntity> branches = branchRepository.findByStatus(BranchStatus.ACTIVE);
            context.append("=== CHI NHÁNH ===\n");
            for (BranchEntity branch : branches) {
                context.append(String.format("- %s (%s) tại %s, %s\n  Loại: %s | SĐT: %s\n",
                        branch.getName(),
                        branch.getType().getDisplayName(),
                        branch.getAddress(),
                        branch.getProvince(),
                        branch.getType().getDisplayName(),
                        branch.getPhoneNumber()
                ));
            }

            // 2. Thông tin loại phòng
            List<RoomTypeEntity> roomTypes = roomTypeRepository.findByStatus(Status.ACTIVE);
            context.append("\n=== LOẠI PHÒNG ===\n");
            for (RoomTypeEntity roomType : roomTypes.stream().limit(10).collect(Collectors.toList())) {
                context.append(String.format("- %s tại chi nhánh %s: %,.0f VNĐ/đêm, sức chứa %d người, giường %s\n",
                        roomType.getName(),
                        roomType.getBranch().getName(),
                        roomType.getPrice(),
                        roomType.getMaxOccupancy(),
                        roomType.getBedType()
                ));
            }

            // 3. Thông tin món ăn
            List<MenuItemEntity> menuItems = menuItemRepository.findByStatusAndIsAvailable(Status.ACTIVE, true);
            context.append("\n=== MÓN ĂN ===\n");
            for (MenuItemEntity item : menuItems.stream().limit(15).collect(Collectors.toList())) {
                context.append(String.format("- %s: %,.0f VNĐ (%s)\n",
                        item.getName(),
                        item.getPrice(),
                        item.getCategory().getName()
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error building context: " + e.getMessage());
            context.append("Dữ liệu đang được cập nhật...\n");
        }

        return context.toString();
    }

    /**
     * ✅ Tạo prompt với instruction chi tiết
     */
    private String buildPrompt(String systemContext, String userMessage) {
        return String.format("""
                BẠN LÀ CHATBOT TƯ VẤN CHO HỆ THỐNG KHÁCH SẠN & NHÀ HÀNG LUXURY.
                
                NHIỆM VỤ:
                - Chỉ trả lời câu hỏi liên quan đến: chi nhánh, phòng, món ăn, đặt phòng, giá cả
                - Nếu hỏi ngoài phạm vi, lịch sự từ chối và hướng dẫn hỏi đúng chủ đề
                - Trả lời ngắn gọn, thân thiện, chuyên nghiệp bằng tiếng Việt
                - Luôn kèm số điện thoại liên hệ nếu cần hỗ trợ thêm
                
                DỮ LIỆU HỆ THỐNG:
                %s
                
                CÂU HỎI KHÁCH HÀNG:
                %s
                
                TRẢ LỜI (tối đa 200 từ, bằng tiếng Việt):
                """, systemContext, userMessage);
    }

    /**
     * ✅ Tạo request body theo format Gemini API v1beta (Google AI Studio)
     */
    private JSONObject createGeminiRequest(String prompt) {
        JSONObject textPart = new JSONObject();
        textPart.put("text", prompt);

        JSONArray parts = new JSONArray();
        parts.put(textPart);

        JSONObject content = new JSONObject();
        content.put("parts", parts);

        JSONArray contents = new JSONArray();
        contents.put(content);

        JSONObject requestBody = new JSONObject();
        requestBody.put("contents", contents);

        // ✅ Generation config
        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 800);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);
        requestBody.put("generationConfig", generationConfig);

        // ✅ Safety settings (tùy chọn)
        JSONArray safetySettings = new JSONArray();
        String[] categories = {
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
        };
        for (String category : categories) {
            JSONObject setting = new JSONObject();
            setting.put("category", category);
            setting.put("threshold", "BLOCK_MEDIUM_AND_ABOVE");
            safetySettings.put(setting);
        }
        requestBody.put("safetySettings", safetySettings);

        return requestBody;
    }

    /**
     * ✅ Parse response từ Gemini
     */
    private String parseResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);

            // ✅ Kiểm tra có candidates không
            if (!json.has("candidates") || json.getJSONArray("candidates").length() == 0) {
                System.err.println("❌ No candidates in response: " + responseBody);
                return "Xin lỗi, tôi không thể tạo câu trả lời phù hợp lúc này.";
            }

            JSONObject candidate = json.getJSONArray("candidates").getJSONObject(0);

            // ✅ Kiểm tra finish reason
            if (candidate.has("finishReason") &&
                    !candidate.getString("finishReason").equals("STOP")) {
                String reason = candidate.getString("finishReason");
                System.err.println("❌ Finish reason: " + reason);
                return "Xin lỗi, câu trả lời bị chặn do chính sách an toàn.";
            }

            // ✅ Lấy text content
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");

            if (parts.length() == 0) {
                return "Xin lỗi, không có nội dung trả lời.";
            }

            String text = parts.getJSONObject(0).getString("text").trim();

            // ✅ Kiểm tra text có rỗng không
            if (text.isEmpty()) {
                return "Xin lỗi, câu trả lời trống. Vui lòng thử lại.";
            }

            return text;

        } catch (Exception e) {
            System.err.println("❌ Parse error: " + e.getMessage());
            System.err.println("Response body: " + responseBody);
            e.printStackTrace();
            return "Xin lỗi, tôi không thể xử lý câu trả lời lúc này. Vui lòng thử lại.";
        }
    }

    /**
     * ✅ Kiểm tra phòng trống
     */
    public String checkRoomAvailability(Long roomTypeId, String checkInStr, String checkOutStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            LocalDate checkIn = LocalDate.parse(checkInStr, formatter);
            LocalDate checkOut = LocalDate.parse(checkOutStr, formatter);

            int available = roomRepository.countAvailableRoomsByTypeAndDateRange(
                    roomTypeId, checkIn, checkOut);

            return available > 0
                    ? String.format("Có %d phòng trống từ %s đến %s", available, checkInStr, checkOutStr)
                    : "Không còn phòng trống trong khoảng thời gian này";
        } catch (DateTimeParseException e) {
            return "Định dạng ngày không hợp lệ. Vui lòng dùng YYYY-MM-DD";
        } catch (Exception e) {
            System.err.println("❌ Error checking room availability: " + e.getMessage());
            return "Không thể kiểm tra phòng trống lúc này.";
        }
    }
}