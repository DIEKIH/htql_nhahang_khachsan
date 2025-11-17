package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.BookingSessionDTO;
import com.example.htql_nhahang_khachsan.dto.QuickOrderRequest;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.*;
import com.example.htql_nhahang_khachsan.repository.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final BookingService bookingService;
    private final ChatbotBookingDraftRepository draftRepository;
    private final WebClient webClient;
    private final String apiKey;
    private final BranchRepository branchRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MenuItemRepository menuItemRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final String modelName;

    // ===== THÊM DEPENDENCIES MỚI CHO MÓN ĂN =====
    private final MenuCategoryRepository menuCategoryRepository; // ✅ THÊM
    private final CartService cartService; // ✅ THÊM
    private final MenuService menuService; // ✅ THÊM (nếu cần)

    private final QuickOrderDraftRepository quickOrderDraftRepository; // ✅ THÊM

    private final OrderService orderService;

    public ChatbotService(WebClient.Builder webClientBuilder,
                          @Value("${gemini.api.key}") String apiKey,
                          @Value("${gemini.model:gemini-1.5-flash}") String modelName,
                          BranchRepository branchRepository,
                          RoomTypeRepository roomTypeRepository,
                          MenuItemRepository menuItemRepository,
                          RoomRepository roomRepository,
                          RoomService roomService,
                          BookingService bookingService,
                          ChatbotBookingDraftRepository draftRepository,
                          MenuCategoryRepository menuCategoryRepository,
                          CartService cartService,
                          MenuService menuService,
                          QuickOrderDraftRepository quickOrderDraftRepository,
                            OrderService orderService
                          ) {
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.branchRepository = branchRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.menuItemRepository = menuItemRepository;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.draftRepository = draftRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.cartService = cartService;
        this.menuService = menuService;
        this.quickOrderDraftRepository = quickOrderDraftRepository; // ✅ THÊM
        this.orderService = orderService;

        System.out.println("✅ Chatbot initialized with model: " + modelName);
    }

    // ✅ PHẦN 1: Regex patterns - LINH HOẠT HƠN
    private static final Pattern BRANCH_LIST_PATTERN = Pattern.compile(
            "(?:danh sách|liệt kê|có những|đưa|cho xem|hiển thị).{0,10}chi nhánh",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Pattern linh hoạt hơn cho "loại phòng ở/tại chi nhánh X"
    private static final Pattern ROOM_TYPE_PATTERN = Pattern.compile(
            "loại phòng.{0,5}(?:ở|tại|của|chi nhánh)\\s+(.+?)(?:\\s+(?:có|là|như|gì)|$)",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Pattern linh hoạt hơn - KHÔNG BẮT BUỘC chi nhánh trong câu
    private static final Pattern AVAILABILITY_PATTERN = Pattern.compile(
            "(?:phòng|loại phòng)?\\s*([\\w\\sÀ-ỹ]+?)\\s*(?:này)?\\s*" +
                    "(?:ngày|từ|vào|tại)?\\s*(\\d{1,2}[/\\s-]\\d{1,2}(?:[/\\s-]\\d{2,4})?)" +
                    "(?:\\s*(?:đến|tới|-|,)\\s*(\\d{1,2}[/\\s-]\\d{1,2}(?:[/\\s-]\\d{2,4})?))?.*" +
                    "(?:có|còn)?\\s*(?:trống|available)?",
            Pattern.CASE_INSENSITIVE
    );


    // ✅ THÊM: Pattern để detect intent đặt phòng
//    private static final Pattern BOOKING_INTENT_PATTERN = Pattern.compile(
//            "(?i).*(đặt|book|booking|đặt phòng|đặt dùm|giúp đặt|book cho).*",
//            Pattern.CASE_INSENSITIVE
//    );

    // ✅ Pattern cho ĐẶT PHÒNG - Phải có từ "phòng"
    private static final Pattern BOOKING_INTENT_PATTERN = Pattern.compile(
            "(?i).*(đặt|book|booking|giúp đặt|đặt dùm|book cho)\\s+(phòng|room).*",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Pattern NAME linh hoạt hơn
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?i)(?:tên\\s*:?\\s*|^)([\\p{L}\\s]{2,50})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );

    // ✅ SỬA: Pattern EMAIL chính xác hơn
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)(?:email\\s*:?\\s*)?([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );

    // ✅ SỬA: Pattern PHONE linh hoạt hơn
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?i)(?:sđt|số điện thoại|phone)\\s*:?\\s*([0-9]{10,11})|\\b(0[0-9]{9,10})\\b"
    );

    // ✅ THÊM VÀO ChatbotService.java - PHẦN PATTERNS

// ===== PATTERNS CHO MÓN ĂN - CẢI TIẾN =====

    // ✅ SỬA: Xem danh sách món ăn
    private static final Pattern MENU_LIST_PATTERN = Pattern.compile(
            "(?:danh sách|liệt kê|có những|xem|cho xem|hiển thị).{0,15}(?:món ăn|menu|thực đơn)",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Xem món theo danh mục
    private static final Pattern MENU_BY_CATEGORY_PATTERN = Pattern.compile(
            "(?:món|menu|thực đơn).{0,10}(?:loại|danh mục|category)\\s+(.+?)(?:\\s+(?:ở|tại|chi nhánh)|$)",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Thêm vào giỏ - LINH HOẠT HƠN
    private static final Pattern ADD_TO_CART_PATTERN = Pattern.compile(
            "(?:thêm|cho|add|cho tôi|tôi muốn).{0,15}(?:món)?\\s*(.+?)(?:\\s*(?:vào giỏ|x\\d+)|$)",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ GIỮ NGUYÊN: Xem giỏ hàng
    private static final Pattern VIEW_CART_PATTERN = Pattern.compile(
            "(?:xem|kiểm tra|check).{0,10}(?:giỏ hàng|giỏ|cart)",
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Đặt món / Checkout - RÕ RÀNG HƠN
    private static final Pattern ORDER_FOOD_PATTERN = Pattern.compile(
            "(?:đặt món|đặt hàng|order|checkout|thanh toán)(?!.*phòng)", // ✅ Loại trừ "phòng"
            Pattern.CASE_INSENSITIVE
    );

    // ✅ SỬA: Tìm món ăn
    private static final Pattern SEARCH_MENU_PATTERN = Pattern.compile(
            "(?:tìm|search|có món|còn món).{0,10}(?:món)?\\s+(.+?)(?:\\s+(?:không|ở|tại)|$)",
            Pattern.CASE_INSENSITIVE
    );



    // Phương thức chính để xử lý tin nhắn, giờ đây có thêm 'history'
    // ✅ PHẦN 2: Method getReply - THÊM context tracking
//    public Mono<String> getReply(String userMessage, String history) {
//        String fullContext = (history != null ? history + "\n" : "") + userMessage;
//
//        System.out.println("=== CHATBOT INPUT ===");
//        System.out.println("Message: " + userMessage);
//        System.out.println("History: " + (history != null ? history.substring(0, Math.min(200, history.length())) : "empty"));
//
//
//
//        // ✅ THÊM: Check xem có draft code trong context không
//        Pattern draftPattern = Pattern.compile("DRAFT\\d+");
//        Matcher draftMatcher = draftPattern.matcher(fullContext);
//
//        if (draftMatcher.find()) {
//            String draftCode = draftMatcher.group();
//            Optional<ChatbotBookingDraftEntity> draftOpt = draftRepository.findByDraftCode(draftCode);
//
//            if (draftOpt.isPresent()) {
//                System.out.println("✅ Found active draft: " + draftCode);
//                return handleBookingProcess(userMessage, draftOpt.get(), fullContext);
//            }
//        }
//
//        // ✅ THÊM: Check nếu user nói "đặt phòng", "book", "đặt luôn"
//        if (userMessage.matches("(?i).*(đặt|book|đặt luôn|đặt ngay|đặt dùm|giúp đặt).*")) {
//            // Tìm draft gần nhất trong context
//            Optional<ChatbotBookingDraftEntity> recentDraft = findRecentDraftFromContext(fullContext);
//
//            if (recentDraft.isPresent()) {
//                System.out.println("✅ User wants to book, found draft: " + recentDraft.get().getDraftCode());
//                return Mono.just(createInfoCollectionResponse(recentDraft.get()));
//            }
//
//            // Nếu không có draft, yêu cầu user check phòng trước
//            return Mono.just(new JSONObject()
//                    .put("reply", "Để đặt phòng, bạn cần kiểm tra phòng trống trước nhé!\n\n" +
//                            "Ví dụ: 'Phòng Standard tại CMT8 từ 25/12 đến 27/12 còn trống không?'")
//                    .toString());
//        }
//
//
//
//
//        // ===== THÊM: CHECK PATTERNS MÓN ĂN TRƯỚC CÁC PATTERN PHÒNG =====
//
//        // 1. Xem giỏ hàng
//        Matcher viewCartMatcher = VIEW_CART_PATTERN.matcher(userMessage);
//        if (viewCartMatcher.find()) {
//            System.out.println("✅ Matched: View Cart");
//            return handleViewCart(fullContext);
//        }
//
//        // 2. Đặt món / Checkout
//        Matcher orderFoodMatcher = ORDER_FOOD_PATTERN.matcher(userMessage);
//        if (orderFoodMatcher.find() && !userMessage.contains("phòng")) {
//            System.out.println("✅ Matched: Order Food");
//            return handleOrderFood(fullContext);
//        }
//
//        // 3. Thêm vào giỏ hàng
//        Matcher addToCartMatcher = ADD_TO_CART_PATTERN.matcher(userMessage);
//        if (addToCartMatcher.find()) {
//            System.out.println("✅ Matched: Add to Cart");
//            String itemName = addToCartMatcher.group(1).trim();
//            return handleAddToCart(itemName, userMessage, fullContext);
//        }
//
//        // 4. Tìm món ăn
//        Matcher searchMenuMatcher = SEARCH_MENU_PATTERN.matcher(userMessage);
//        if (searchMenuMatcher.find()) {
//            System.out.println("✅ Matched: Search Menu");
//            String keyword = searchMenuMatcher.group(1).trim();
//            return handleSearchMenu(keyword, fullContext);
//        }
//
//        // 5. Xem món ăn theo danh mục
//        Matcher menuByCategoryMatcher = MENU_BY_CATEGORY_PATTERN.matcher(userMessage);
//        if (menuByCategoryMatcher.find()) {
//            System.out.println("✅ Matched: Menu by Category");
//            String categoryName = menuByCategoryMatcher.group(1).trim();
//            return handleMenuByCategory(categoryName, fullContext);
//        }
//
//        // 6. Xem danh sách món ăn
//        Matcher menuListMatcher = MENU_LIST_PATTERN.matcher(userMessage);
//        if (menuListMatcher.find()) {
//            System.out.println("✅ Matched: Menu List");
//            return handleGetMenuList(fullContext);
//        }
//
//        // Check các pattern theo thứ tự
//        Matcher branchListMatcher = BRANCH_LIST_PATTERN.matcher(userMessage);
//        if (branchListMatcher.find()) {
//            System.out.println("✅ Matched: Branch List");
//            return handleGetBranchList();
//        }
//
//
//
//        Matcher roomTypeMatcher = ROOM_TYPE_PATTERN.matcher(userMessage);
//        if (roomTypeMatcher.find()) {
//            System.out.println("✅ Matched: Room Type List");
//            String branchName = roomTypeMatcher.group(1).trim();
//            return handleGetRoomTypesByBranch(branchName);
//        }
//
//        // ✅ QUAN TRỌNG: Check availability trước khi dùng Gemini
//        Matcher availabilityMatcher = AVAILABILITY_PATTERN.matcher(userMessage);
//        if (availabilityMatcher.find() && userMessage.matches(".*\\d{1,2}[/\\s-]\\d{1,2}.*")) {
//            System.out.println("✅ Matched: Availability Check");
//            String roomTypeName = availabilityMatcher.group(1).trim();
//            String checkInStr = availabilityMatcher.group(2).trim();
//            String checkOutStr = availabilityMatcher.group(3) != null ? availabilityMatcher.group(3).trim() : null;
//
//            System.out.println("Extracted - Room: '" + roomTypeName + "', In: " + checkInStr + ", Out: " + checkOutStr);
//
//            return handleCheckRoomAvailability(roomTypeName, checkInStr, checkOutStr, fullContext);
//        }
//
//        // ✅ THÊM: Check nếu đang trong process booking
//        Optional<ChatbotBookingDraftEntity> activeDraftOpt =
//                findActiveDraftFromContext(fullContext);
//
//        if (activeDraftOpt.isPresent()) {
//            return handleBookingProcess(userMessage, activeDraftOpt.get(), fullContext);
//        }
//
//        // ✅ THÊM: Check booking intent sau khi check availability
//        // Tạo matcher mới để lấy lại thông tin
//
//
//        if (availabilityMatcher.find()) {
//            String roomTypeName = availabilityMatcher.group(1).trim();
//            String checkInStr = availabilityMatcher.group(2).trim();
//            String checkOutStr = availabilityMatcher.group(3) != null ? availabilityMatcher.group(3).trim() : null;
//
//            return handleCheckRoomAvailability(roomTypeName, checkInStr, checkOutStr, fullContext)
//                    .flatMap(response -> {
//                        JSONObject jsonResponse = new JSONObject(response);
//                        if (jsonResponse.optBoolean("available", false)) {
//                            return addBookingSuggestion(response, roomTypeName, checkInStr, checkOutStr);
//                        }
//                        return Mono.just(response);
//                    });
//        }
//
//
//        // ✅ THÊM: Check direct booking intent
//        Matcher bookingMatcher = BOOKING_INTENT_PATTERN.matcher(userMessage);
//        if (bookingMatcher.find()) {
//            return handleBookingIntent(userMessage, fullContext);
//        }
//
//
//
//
//        System.out.println("⚠️ No pattern matched, using Gemini");
//        return getGenericReply(userMessage, history);
//    }

    // ✅ SỬA: getReply() trong ChatbotService.java

//    public Mono<String> getReply(String userMessage, String history) {
//        String fullContext = (history != null ? history : "") + "\n" + userMessage;
//
//        System.out.println("=== CHATBOT INPUT ===");
//        System.out.println("Message: " + userMessage);
//        System.out.println("History length: " + (history != null ? history.length() : 0));
//
//        // ===== PHẦN 1: CHECK ĐẶT PHÒNG DRAFT =====
//        Pattern draftPattern = Pattern.compile("DRAFT\\d+");
//        Matcher draftMatcher = draftPattern.matcher(fullContext);
//
//        if (draftMatcher.find()) {
//            String draftCode = draftMatcher.group();
//            Optional<ChatbotBookingDraftEntity> draftOpt = draftRepository.findByDraftCode(draftCode);
//
//            if (draftOpt.isPresent()) {
//                System.out.println("✅ Found active draft: " + draftCode);
//                return handleBookingProcess(userMessage, draftOpt.get(), fullContext);
//            }
//        }
//
//        // ===== PHẦN 2: PATTERNS MÓN ĂN - ƯU TIÊN CAO =====
//
//        // 1. ✅ Xem giỏ hàng
//        Matcher viewCartMatcher = VIEW_CART_PATTERN.matcher(userMessage);
//        if (viewCartMatcher.find()) {
//            System.out.println("✅ Pattern: View Cart");
//            return handleViewCart(fullContext);
//        }
//
//        // 2. ✅ Đặt món / Checkout (chỉ khi KHÔNG phải đặt phòng)
//        Matcher orderFoodMatcher = ORDER_FOOD_PATTERN.matcher(userMessage);
//        if (orderFoodMatcher.find() && !userMessage.toLowerCase().contains("phòng")) {
//            System.out.println("✅ Pattern: Order Food / Checkout");
//            return handleOrderFood(fullContext);
//        }
//
//        // 3. ✅ Thêm vào giỏ hàng
//        Matcher addToCartMatcher = ADD_TO_CART_PATTERN.matcher(userMessage);
//        if (addToCartMatcher.find()) {
//            System.out.println("✅ Pattern: Add to Cart");
//            String itemName = addToCartMatcher.group(1).trim();
//            return handleAddToCart(itemName, userMessage, fullContext);
//        }
//
//        // 4. ✅ Tìm món ăn
//        Matcher searchMenuMatcher = SEARCH_MENU_PATTERN.matcher(userMessage);
//        if (searchMenuMatcher.find()) {
//            System.out.println("✅ Pattern: Search Menu");
//            String keyword = searchMenuMatcher.group(1).trim();
//            return handleSearchMenu(keyword, fullContext);
//        }
//
//        // 5. ✅ Xem món ăn theo danh mục
//        Matcher menuByCategoryMatcher = MENU_BY_CATEGORY_PATTERN.matcher(userMessage);
//        if (menuByCategoryMatcher.find()) {
//            System.out.println("✅ Pattern: Menu by Category");
//            String categoryName = menuByCategoryMatcher.group(1).trim();
//            return handleMenuByCategory(categoryName, fullContext);
//        }
//
//        // 6. ✅ Xem danh sách món ăn
//        Matcher menuListMatcher = MENU_LIST_PATTERN.matcher(userMessage);
//        if (menuListMatcher.find()) {
//            System.out.println("✅ Pattern: Menu List");
//            return handleGetMenuList(fullContext);
//        }
//
//        // ===== PHẦN 3: PATTERNS PHÒNG =====
//
//        // 7. ✅ Danh sách chi nhánh
//        Matcher branchListMatcher = BRANCH_LIST_PATTERN.matcher(userMessage);
//        if (branchListMatcher.find()) {
//            System.out.println("✅ Pattern: Branch List");
//            return handleGetBranchList();
//        }
//
//        // 8. ✅ Loại phòng theo chi nhánh
//        Matcher roomTypeMatcher = ROOM_TYPE_PATTERN.matcher(userMessage);
//        if (roomTypeMatcher.find()) {
//            System.out.println("✅ Pattern: Room Type List");
//            String branchName = roomTypeMatcher.group(1).trim();
//            return handleGetRoomTypesByBranch(branchName);
//        }
//
//        // 9. ✅ Kiểm tra phòng trống
//        Matcher availabilityMatcher = AVAILABILITY_PATTERN.matcher(userMessage);
//        if (availabilityMatcher.find() && userMessage.matches(".*\\d{1,2}[/\\s-]\\d{1,2}.*")) {
//            System.out.println("✅ Pattern: Availability Check");
//            String roomTypeName = availabilityMatcher.group(1).trim();
//            String checkInStr = availabilityMatcher.group(2).trim();
//            String checkOutStr = availabilityMatcher.group(3) != null ?
//                    availabilityMatcher.group(3).trim() : null;
//
//            return handleCheckRoomAvailability(roomTypeName, checkInStr, checkOutStr, fullContext)
//                    .flatMap(response -> {
//                        JSONObject jsonResponse = new JSONObject(response);
//                        if (jsonResponse.optBoolean("available", false)) {
//                            return addBookingSuggestion(response, roomTypeName, checkInStr, checkOutStr);
//                        }
//                        return Mono.just(response);
//                    });
//        }
//
//        // 10. ✅ Đặt phòng intent
//        if (userMessage.matches("(?i).*(đặt|book|đặt luôn|đặt ngay|đặt dùm|giúp đặt).*phòng.*")) {
//            System.out.println("✅ Pattern: Booking Intent");
//            return handleBookingIntent(userMessage, fullContext);
//        }
//
//        // 11. ✅ Check đang trong process booking
//        Optional<ChatbotBookingDraftEntity> activeDraftOpt = findActiveDraftFromContext(fullContext);
//        if (activeDraftOpt.isPresent()) {
//            return handleBookingProcess(userMessage, activeDraftOpt.get(), fullContext);
//        }
//
//        // ===== PHẦN 4: FALLBACK TO GEMINI =====
//        System.out.println("⚠️ No pattern matched, using Gemini");
//        return getGenericReply(userMessage, history);
//    }

    // ✅ THAY THẾ TOÀN BỘ METHOD getReply() TRONG ChatbotService.java

    public Mono<String> getReply(String userMessage, String history) {
        String fullContext = (history != null ? history : "") + "\n" + userMessage;

        System.out.println("=== CHATBOT INPUT ===");
        System.out.println("Message: " + userMessage);
        System.out.println("History length: " + (history != null ? history.length() : 0));

        // ✅ THÊM: Normalize message để dễ match
        String normalizedMessage = userMessage.toLowerCase().trim();

        // ===== PHẦN 1: CHECK ĐẶT PHÒNG DRAFT =====
        Pattern draftPattern = Pattern.compile("DRAFT\\d+");
        Matcher draftMatcher = draftPattern.matcher(fullContext);

        if (draftMatcher.find()) {
            String draftCode = draftMatcher.group();
            Optional<ChatbotBookingDraftEntity> draftOpt = draftRepository.findByDraftCode(draftCode);

            if (draftOpt.isPresent()) {
                System.out.println("✅ Found active draft: " + draftCode);
                return handleBookingProcess(userMessage, draftOpt.get(), fullContext);
            }
        }

        // ===== PHẦN 2: PATTERNS MÓN ĂN - ƯU TIÊN CAO =====

        // ✅ SỬA: 1. XEM GIỎ HÀNG - Dùng String matching
        if (normalizedMessage.matches("(?i).*(xem|kiểm tra|check).{0,10}(giỏ hàng|giỏ|cart).*")) {
            System.out.println("✅ Pattern: View Cart");
            return handleViewCart(fullContext);
        }

        // ✅ SỬA: 2. ĐẶT MÓN / CHECKOUT - Loại trừ "phòng"
        if (normalizedMessage.matches("(?i).*(đặt món|đặt hàng|order|checkout|thanh toán).*")
                && !normalizedMessage.contains("phòng")) {
            System.out.println("✅ Pattern: Order Food / Checkout");
            return handleOrderFood(fullContext);
        }

        // ✅ SỬA: Thêm vào giỏ - CHÍNH XÁC HƠN
        if (normalizedMessage.matches("(?i).*(thêm|cho|add).+(vào giỏ|giỏ hàng|cart).*")) {
            System.out.println("✅ Pattern: Add to Cart");

            // Extract tên món - LINH HOẠT HƠN
            Pattern itemPattern = Pattern.compile(
                    "(?i)(?:thêm|cho|add)\\s+(.+?)(?:\\s+(?:vào giỏ|giỏ hàng|cart)|$)"
            );
            Matcher itemMatcher = itemPattern.matcher(userMessage);

            if (itemMatcher.find()) {
                String itemName = itemMatcher.group(1).trim()
                        .replaceAll("(?i)\\s*vào\\s*giỏ.*", "")
                        .replaceAll("(?i)\\s*giỏ\\s*hàng.*", "")
                        .replaceAll("(?i)\\s*x\\d+", "")
                        .trim();

                System.out.println("Extracted item: '" + itemName + "'");
                return handleAddToCart(itemName, userMessage, fullContext);
            }
        }

        // ✅ THÊM: Check Quick Order draft trước
        Pattern qoDraftPattern = Pattern.compile("QOD\\d+");
        Matcher qoDraftMatcher = qoDraftPattern.matcher(fullContext);

        if (qoDraftMatcher.find()) {
            String draftCode = qoDraftMatcher.group();
            Optional<QuickOrderDraftEntity> qoDraftOpt =
                    quickOrderDraftRepository.findByDraftCode(draftCode);

            if (qoDraftOpt.isPresent() && !qoDraftOpt.get().isExpired()) {
                QuickOrderDraftEntity draft = qoDraftOpt.get();

                // ✅ Nếu đã có đủ info và user nói "thanh toán"
                if (normalizedMessage.matches("(?i).*(thanh toán|chuyển.*thanh toán|payment).*")) {
                    if (draft.isInfoComplete()) {
                        return processQuickOrderPayment(draft);
                    }
                }

                // ✅ Tiếp tục thu thập info
                System.out.println("✅ Found active Quick Order draft: " + draftCode);
                return handleQuickOrderInfoCollection(userMessage, draft);
            }
        }


        // ✅ SỬA: Pattern linh hoạt hơn cho "đặt món"
        if (normalizedMessage.matches("(?i).*(đặt|order).*(món|giúp|dùm|cho).*")
                && !normalizedMessage.contains("phòng")) {

            System.out.println("✅ Pattern: Quick Order Food");

            // Extract tên món
            String itemName = extractMenuItemName(userMessage, fullContext);

            if (itemName != null && !itemName.isEmpty()) {
                return handleQuickOrderFood(itemName, userMessage, fullContext);
            } else {
                return Mono.just(new JSONObject()
                        .put("reply", "Bạn muốn đặt món gì? Vui lòng nói rõ tên món ạ!")
                        .toString());
            }
        }

        // ✅ SỬA: 4. TÌM MÓN ĂN
        if (normalizedMessage.matches("(?i).*(tìm|search|có món).+")) {
            System.out.println("✅ Pattern: Search Menu");

            Pattern searchPattern = Pattern.compile(
                    "(?i)(?:tìm|search|có món)\\s+(.+?)(?:\\s+không|\\s+ở|\\s+tại|$)"
            );
            Matcher searchMatcher = searchPattern.matcher(normalizedMessage);

            if (searchMatcher.find()) {
                String keyword = searchMatcher.group(1).trim();
                return handleSearchMenu(keyword, fullContext);
            }
        }

        // ✅ SỬA: 5. XEM MÓN THEO DANH MỤC - Cải thiện pattern
        if (normalizedMessage.matches("(?i).*(?:xem|món).{0,15}(tráng miệng|món chính|khai vị).*")) {
            System.out.println("✅ Pattern: Menu by Category");

            Pattern catPattern = Pattern.compile(
                    "(?i)(tráng miệng|món chính|khai vị)"
            );
            Matcher catMatcher = catPattern.matcher(normalizedMessage);

            if (catMatcher.find()) {
                String categoryName = catMatcher.group(1);
                return handleMenuByCategory(categoryName, fullContext);
            }
        }

        // ✅ SỬA: 6. XEM DANH SÁCH MÓN ĂN
        if (normalizedMessage.matches("(?i).*(danh sách|liệt kê|xem|hiển thị).{0,15}(món ăn|menu|thực đơn).*")) {
            System.out.println("✅ Pattern: Menu List");
            return handleGetMenuList(fullContext);
        }

        // ===== PHẦN 3: PATTERNS PHÒNG =====

        // ✅ SỬA: 7. DANH SÁCH CHI NHÁNH
        if (normalizedMessage.matches("(?i).*(danh sách|liệt kê|xem|cho.*xem).{0,10}chi nhánh.*")) {
            System.out.println("✅ Pattern: Branch List");
            return handleGetBranchList();
        }

        // ✅ GIỮ NGUYÊN: 8. LOẠI PHÒNG THEO CHI NHÁNH
        Matcher roomTypeMatcher = ROOM_TYPE_PATTERN.matcher(userMessage);
        if (roomTypeMatcher.find()) {
            System.out.println("✅ Pattern: Room Type List");
            String branchName = roomTypeMatcher.group(1).trim();
            return handleGetRoomTypesByBranch(branchName);
        }

        // ✅ GIỮ NGUYÊN: 9. KIỂM TRA PHÒNG TRỐNG
        Matcher availabilityMatcher = AVAILABILITY_PATTERN.matcher(userMessage);
        if (availabilityMatcher.find() && userMessage.matches(".*\\d{1,2}[/\\s-]\\d{1,2}.*")) {
            System.out.println("✅ Pattern: Availability Check");
            String roomTypeName = availabilityMatcher.group(1).trim();
            String checkInStr = availabilityMatcher.group(2).trim();
            String checkOutStr = availabilityMatcher.group(3) != null ?
                    availabilityMatcher.group(3).trim() : null;

            return handleCheckRoomAvailability(roomTypeName, checkInStr, checkOutStr, fullContext)
                    .flatMap(response -> {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.optBoolean("available", false)) {
                            return addBookingSuggestion(response, roomTypeName, checkInStr, checkOutStr);
                        }
                        return Mono.just(response);
                    });
        }

        // ✅ GIỮ NGUYÊN: 10. ĐẶT PHÒNG INTENT
        if (userMessage.matches("(?i).*(đặt|book|đặt luôn|đặt ngay|đặt dùm|giúp đặt).*phòng.*")) {
            System.out.println("✅ Pattern: Booking Intent");
            return handleBookingIntent(userMessage, fullContext);
        }

        // ✅ GIỮ NGUYÊN: 11. CHECK ĐANG TRONG PROCESS BOOKING
        Optional<ChatbotBookingDraftEntity> activeDraftOpt = findActiveDraftFromContext(fullContext);
        if (activeDraftOpt.isPresent()) {
            return handleBookingProcess(userMessage, activeDraftOpt.get(), fullContext);
        }

        // ===== PHẦN 4: FALLBACK TO GEMINI =====
        System.out.println("⚠️ No pattern matched, using Gemini");
        return getGenericReply(userMessage, history);
    }

    // ===== HANDLER: ĐẶT MÓN NHANH =====
    // ✅ SỬA: handleQuickOrderFood - THÊM draft code vào reply

    private Mono<String> handleQuickOrderFood(String itemName, String message, String context) {
        System.out.println("=== QUICK ORDER FOOD ===");
        System.out.println("Item: " + itemName);

        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);
        if (branchOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", "Bạn muốn đặt món tại chi nhánh nào?")
                    .toString());
        }

        BranchEntity branch = branchOpt.get();
        String normalizedItemName = removeVietnameseTones(itemName.toLowerCase().trim());
        Optional<MenuItemEntity> itemOpt = findMenuItemByName(normalizedItemName, branch.getId());

        if (itemOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", String.format("❌ Không tìm thấy món '%s'", itemName))
                    .toString());
        }

        MenuItemEntity menuItem = itemOpt.get();
        int quantity = extractQuantityFromMessage(message);

        QuickOrderDraftEntity draft = QuickOrderDraftEntity.builder()
                .sessionId(UUID.randomUUID().toString())
                .menuItem(menuItem)
                .branch(branch)
                .quantity(quantity)
                .unitPrice(menuItem.getPrice())
                .currentStep(QuickOrderStep.ITEM_SELECTED)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        draft = quickOrderDraftRepository.save(draft);
        System.out.println("✅ Created draft: " + draft.getDraftCode());

        JSONObject response = new JSONObject();

        // ✅ SỬA: THÊM draft code vào reply để frontend lưu vào history
        response.put("reply", String.format(
                "✅ **Đặt món nhanh**\n\n" +
                        "🍽️ %s x%d\n💰 %,d₫\n\n" +
                        "📋 Tôi cần:\n1️⃣ Họ tên\n2️⃣ SĐT\n3️⃣ Địa chỉ\n\n" +
                        "Gửi:\n```\nTên: ...\nSĐT: ...\nĐịa chỉ: ...\n```\n\n" +
                        "**[Mã đơn: %s]**", // ✅ THÊM dòng này để track
                menuItem.getName(),
                quantity,
                menuItem.getPrice().longValue() * quantity,
                draft.getDraftCode() // ✅ QUAN TRỌNG
        ));

        response.put("type", "info_collection");
        response.put("draftCode", draft.getDraftCode()); // Giữ lại để tương thích

        return Mono.just(response.toString());
    }

    // ✅ THÊM: Helper extract tên món từ message
    private String extractMenuItemName(String message, String context) {
        // Pattern 1: "đặt món X"
        Pattern p1 = Pattern.compile("(?i)(?:đặt|order).*?món\\s+([\\p{L}\\s]+?)(?:\\s|$)",
                Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m1 = p1.matcher(message);
        if (m1.find()) {
            return m1.group(1).trim();
        }

        // Pattern 2: "nhờ bạn đặt giúp món X"
        Pattern p2 = Pattern.compile("(?i)(?:giúp|dùm|cho).*món\\s+([\\p{L}\\s]+?)(?:\\s|$)",
                Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m2 = p2.matcher(message);
        if (m2.find()) {
            return m2.group(1).trim();
        }

        // Pattern 3: Lấy từ context (món đã được đề cập gần đây)
        Pattern p3 = Pattern.compile("(?i)([\\p{L}\\s]{3,20})\\s*-\\s*\\d{1,3},\\d{3}",
                Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m3 = p3.matcher(context);

        String lastMenuItem = null;
        while (m3.find()) {
            lastMenuItem = m3.group(1).trim();
        }

        return lastMenuItem;
    }

    // ✅ SỬA: handleQuickOrderInfoCollection - CẢI THIỆN EXTRACT

    private Mono<String> handleQuickOrderInfoCollection(String userMessage, QuickOrderDraftEntity draft) {
        System.out.println("=== COLLECTING QUICK ORDER INFO ===");
        System.out.println("Draft: " + draft.getDraftCode());
        System.out.println("Current step: " + draft.getCurrentStep());

        boolean updated = false;
        String normalizedMessage = userMessage.trim();

        // ===== THU THẬP TÊN =====
        if (draft.getGuestName() == null || draft.getGuestName().trim().isEmpty()) {
            // Pattern linh hoạt hơn
            Pattern namePattern = Pattern.compile(
                    "([\\p{L}\\s]{2,50})(?=\\s*0\\d{9}|$)",
                    Pattern.UNICODE_CHARACTER_CLASS
            );
            Matcher nameMatcher = namePattern.matcher(normalizedMessage);

            if (nameMatcher.find()) {
                String name = nameMatcher.group(1).trim();

                // ✅ Loại bỏ các từ khóa không phải tên
                name = name.replaceAll("(?i)(tên|sđt|số|địa chỉ|:)", "").trim();

                if (name.length() >= 2 && name.matches(".*[\\p{L}]{2,}.*")) {
                    draft.setGuestName(name);
                    updated = true;
                    System.out.println("✅ Extracted name: '" + name + "'");
                }
            }
        }

        // ===== THU THẬP SĐT =====
        if (draft.getGuestPhone() == null || draft.getGuestPhone().trim().isEmpty()) {
            Pattern phonePattern = Pattern.compile("(0\\d{9,10})");
            Matcher phoneMatcher = phonePattern.matcher(normalizedMessage);

            if (phoneMatcher.find()) {
                String phone = phoneMatcher.group(1).trim();
                draft.setGuestPhone(phone);
                updated = true;
                System.out.println("✅ Extracted phone: '" + phone + "'");
            }
        }

        // ===== THU THẬP ĐỊA CHỈ =====
        if (draft.getGuestAddress() == null || draft.getGuestAddress().trim().isEmpty()) {
            String addressCandidate = normalizedMessage;

            // ✅ Loại bỏ tên và SĐT đã extract
            if (draft.getGuestName() != null) {
                addressCandidate = addressCandidate.replace(draft.getGuestName(), "");
            }
            if (draft.getGuestPhone() != null) {
                addressCandidate = addressCandidate.replace(draft.getGuestPhone(), "");
            }

            // ✅ Loại bỏ các từ khóa
            addressCandidate = addressCandidate
                    .replaceAll("(?i)(tên|sđt|số điện thoại|địa chỉ|:)", "")
                    .trim();

            // ✅ Check độ dài tối thiểu
            if (addressCandidate.length() >= 8) { // Giảm từ 10 xuống 8
                draft.setGuestAddress(addressCandidate);
                updated = true;
                System.out.println("✅ Extracted address: '" + addressCandidate + "'");
            }
        }

        // ===== LƯU DRAFT =====
        if (updated) {
            draft.setCurrentStep(QuickOrderStep.INFO_COLLECTING);
            draft = quickOrderDraftRepository.save(draft);
            System.out.println("💾 Saved draft");
        }

        // ===== ✅ CHECK ĐỦ INFO =====
        System.out.println("=== CHECKING IF INFO COMPLETE ===");
        System.out.println("Name: " + draft.getGuestName());
        System.out.println("Phone: " + draft.getGuestPhone());
        System.out.println("Address: " + draft.getGuestAddress());

        boolean isComplete = draft.isInfoComplete();
        System.out.println("Is complete: " + isComplete);

        if (isComplete) {
            try {
                System.out.println("=== ✅ INFO COMPLETE - CREATING ORDER ===");

                // ✅ Tạo order request
                QuickOrderRequest orderRequest = QuickOrderRequest.builder()
                        .menuItemId(draft.getMenuItem().getId())
                        .branchId(draft.getBranch().getId())
                        .quantity(draft.getQuantity())
                        .unitPrice(draft.getUnitPrice())
                        .menuItemName(draft.getMenuItem().getName())
                        .menuItemImage(draft.getMenuItem().getImageUrl())
                        .customerName(draft.getGuestName())
                        .customerPhone(draft.getGuestPhone())
                        .customerAddress(draft.getGuestAddress())
                        .orderNotes(draft.getOrderNotes())
                        .paymentMethod("PENDING")
                        .build();

                // ✅ TẠO ORDER
                Long orderId = orderService.createQuickOrderFromChatbot(orderRequest);

                System.out.println("✅✅✅ Order created with ID: " + orderId);

                // ✅ Update draft
                draft.setCurrentStep(QuickOrderStep.ORDER_CREATED);
                quickOrderDraftRepository.save(draft);

                // ✅ Tính tổng tiền
                BigDecimal subtotal = draft.getUnitPrice()
                        .multiply(BigDecimal.valueOf(draft.getQuantity()));

                BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10"))
                        .setScale(0, RoundingMode.HALF_UP);

                BigDecimal vat = subtotal.add(serviceCharge)
                        .multiply(new BigDecimal("0.08"))
                        .setScale(0, RoundingMode.HALF_UP);

                BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

                // ✅ TẠO RESPONSE VỚI BUTTON
                JSONObject response = new JSONObject();

                response.put("reply", String.format(
                        "✅ **Xác nhận đặt món thành công!**\n\n" +
                                "📋 **Chi tiết đơn hàng:**\n" +
                                "🍽️ %s x%d\n" +
                                "💰 Tạm tính: %,d₫\n" +
                                "💵 Phí phục vụ (10%%): %,d₫\n" +
                                "💵 VAT (8%%): %,d₫\n" +
                                "💳 **Tổng cộng: %,d₫**\n\n" +
                                "📦 **Thông tin giao hàng:**\n" +
                                "👤 %s\n" +
                                "📱 %s\n" +
                                "📍 %s\n\n" +
                                "👇 **Nhấn nút bên dưới để chọn phương thức thanh toán:**",
                        draft.getMenuItem().getName(),
                        draft.getQuantity(),
                        subtotal.longValue(),
                        serviceCharge.longValue(),
                        vat.longValue(),
                        totalAmount.longValue(),
                        draft.getGuestName(),
                        draft.getGuestPhone(),
                        draft.getGuestAddress()
                ));

                response.put("type", "list_with_buttons");

                // ✅ BUTTON THANH TOÁN
                JSONArray buttons = new JSONArray();
                buttons.put(new JSONObject()
                        .put("name", "💳 Chọn phương thức thanh toán")
                        .put("url", "/checkout/quick-order/payment/" + orderId));

                response.put("data", buttons);

                return Mono.just(response.toString());

            } catch (Exception e) {
                System.err.println("❌❌❌ Error creating order: " + e.getMessage());
                e.printStackTrace();

                return Mono.just(new JSONObject()
                        .put("reply", "❌ Có lỗi khi tạo đơn hàng: " + e.getMessage())
                        .toString());
            }
        } else {
            // ✅ Chưa đủ info
            System.out.println("⏳ Still missing info");
            return Mono.just(createNextQuickOrderInfoRequest(draft));
        }
    }



    // ✅ THÊM: Xử lý tạo order và trả về response với button
    private Mono<String> processQuickOrderPayment(QuickOrderDraftEntity draft) {
        try {
            System.out.println("=== CREATING QUICK ORDER ===");

            // ✅ Tạo order request
            QuickOrderRequest orderRequest = QuickOrderRequest.builder()
                    .menuItemId(draft.getMenuItem().getId())
                    .branchId(draft.getBranch().getId())
                    .quantity(draft.getQuantity())
                    .unitPrice(draft.getUnitPrice())
                    .menuItemName(draft.getMenuItem().getName())
                    .menuItemImage(draft.getMenuItem().getImageUrl())
                    .customerName(draft.getGuestName())
                    .customerPhone(draft.getGuestPhone())
                    .customerAddress(draft.getGuestAddress())
                    .orderNotes(draft.getOrderNotes())
                    .paymentMethod("PENDING") // Chưa chọn
                    .build();

            // ✅ Tạo order
            Long orderId = orderService.createQuickOrderFromChatbot(orderRequest);

            // ✅ Update draft
            draft.setCurrentStep(QuickOrderStep.ORDER_CREATED);
            quickOrderDraftRepository.save(draft);

            System.out.println("✅ Order created: " + orderId);

            // ✅ Tính toán tổng tiền
            BigDecimal subtotal = draft.getUnitPrice()
                    .multiply(BigDecimal.valueOf(draft.getQuantity()));

            BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10"))
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal vat = subtotal.add(serviceCharge).multiply(new BigDecimal("0.08"))
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

            // ✅ Tạo response với BUTTON THANH TOÁN
            JSONObject response = new JSONObject();
            response.put("reply", String.format(
                    "✅ **Xác nhận đặt món**\n\n" +
                            "🍽️ %s x%d\n" +
                            "💰 Tạm tính: %,d₫\n" +
                            "💵 Phí phục vụ (10%%): %,d₫\n" +
                            "💵 VAT (8%%): %,d₫\n" +
                            "💳 **Tổng cộng: %,d₫**\n\n" +
                            "📋 **Thông tin giao hàng:**\n" +
                            "👤 %s\n" +
                            "📱 %s\n" +
                            "📍 %s\n\n" +
                            "👇 **Nhấn nút bên dưới để chọn phương thức thanh toán!**",
                    draft.getMenuItem().getName(),
                    draft.getQuantity(),
                    subtotal.longValue(),
                    serviceCharge.longValue(),
                    vat.longValue(),
                    totalAmount.longValue(),
                    draft.getGuestName(),
                    draft.getGuestPhone(),
                    draft.getGuestAddress()
            ));

            response.put("type", "list_with_buttons");

            // ✅ QUAN TRỌNG: Button thanh toán
            JSONArray buttons = new JSONArray();
            buttons.put(new JSONObject()
                    .put("name", "💳 Chọn phương thức thanh toán")
                    .put("url", "/checkout/quick-order/payment/" + orderId));

            response.put("data", buttons);

            return Mono.just(response.toString());

        } catch (Exception e) {
            System.err.println("❌ Error creating order: " + e.getMessage());
            e.printStackTrace();

            return Mono.just(new JSONObject()
                    .put("reply", "❌ Có lỗi khi tạo đơn hàng: " + e.getMessage() +
                            "\n\nVui lòng thử lại hoặc liên hệ: 0324245325")
                    .toString());
        }
    }

    // ✅ THÊM: Method tạo message yêu cầu info còn thiếu
    private String createNextQuickOrderInfoRequest(QuickOrderDraftEntity draft) {
        JSONObject response = new JSONObject();
        StringBuilder message = new StringBuilder();

        message.append("📋 **Thông tin đã có:**\n");

        if (draft.getGuestName() != null) {
            message.append("✅ Tên: ").append(draft.getGuestName()).append("\n");
        } else {
            message.append("⏳ Tên: Chưa có\n");
        }

        if (draft.getGuestPhone() != null) {
            message.append("✅ SĐT: ").append(draft.getGuestPhone()).append("\n");
        } else {
            message.append("⏳ SĐT: Chưa có\n");
        }

        if (draft.getGuestAddress() != null) {
            message.append("✅ Địa chỉ: ").append(draft.getGuestAddress()).append("\n");
        } else {
            message.append("⏳ Địa chỉ: Chưa có\n");
        }

        message.append("\n❓ **Cần thêm:**\n");

        List<String> missing = new ArrayList<>();
        if (draft.getGuestName() == null) missing.add("• Họ tên");
        if (draft.getGuestPhone() == null) missing.add("• Số điện thoại (10-11 số)");
        if (draft.getGuestAddress() == null) missing.add("• Địa chỉ giao hàng");

        missing.forEach(item -> message.append(item).append("\n"));

        message.append("\n💬 Vui lòng gửi thông tin còn thiếu!");

        response.put("reply", message.toString());
        response.put("draftCode", draft.getDraftCode());
        response.put("type", "info_collection");

        return response.toString();
    }

    // ✅ SỬA: extractBranchFromContext - TÌM CHÍNH XÁC HƠN
    private Optional<BranchEntity> extractBranchFromContext(String context) {
        List<BranchEntity> allBranches = branchRepository.findByStatus(BranchStatus.ACTIVE);
        String normalizedContext = removeVietnameseTones(context.toLowerCase());

        System.out.println("🔍 Searching branch in context: " + context.substring(0, Math.min(100, context.length())));

        for (BranchEntity branch : allBranches) {
            String branchName = removeVietnameseTones(branch.getName().toLowerCase());
            String branchAddress = removeVietnameseTones(branch.getAddress().toLowerCase());
            String branchProvince = removeVietnameseTones(branch.getProvince().toLowerCase());

            // Check nhiều patterns
            if (normalizedContext.contains(branchName) ||
                    normalizedContext.contains(branchAddress) ||
                    normalizedContext.contains(branchProvince) ||
                    // Check tên rút gọn VD: "CMT8", "Trà Nóc"
                    (branchAddress.contains("cmt8") && normalizedContext.contains("cmt8")) ||
                    (branchAddress.contains("tra noc") && normalizedContext.contains("tra noc")) ||
                    (branch.getName().toLowerCase().contains("diek") && normalizedContext.contains("diek"))) {

                System.out.println("✅ Found branch: " + branch.getName());
                return Optional.of(branch);
            }
        }

        System.out.println("❌ No branch found in context");
        return Optional.empty();
    }



    private Mono<String> handleGetBranchList() {
        List<BranchEntity> branches = branchRepository.findByStatus(BranchStatus.ACTIVE);
        if (branches.isEmpty()) {
            return Mono.just("{\"reply\": \"Xin lỗi, hiện tại chúng tôi chưa có thông tin về chi nhánh nào.\"}");
        }

        JSONObject response = new JSONObject();
        response.put("reply", "Chúng tôi hân hạnh phục vụ quý khách tại các chi nhánh sau:");
        response.put("type", "list_with_buttons");

        JSONArray data = new JSONArray();
        for (BranchEntity branch : branches) {
            JSONObject branchJson = new JSONObject();
            branchJson.put("name", String.format("%s - %s", branch.getName(), branch.getProvince()));
            branchJson.put("url", "/branches/" + branch.getId()); // Cập nhật URL
            data.put(branchJson);
        }
        response.put("data", data);

        return Mono.just(response.toString());
    }

    // ✅ PHẦN 3: SỬA handleGetRoomTypesByBranch - THÊM BUTTONS
    private Mono<String> handleGetRoomTypesByBranch(String branchName) {
        System.out.println("=== SEARCHING ROOM TYPES FOR BRANCH: " + branchName);

        Optional<BranchEntity> branchOpt = branchRepository.findActiveByKeyword(branchName).stream().findFirst();

        if (branchOpt.isEmpty()) {
            return Mono.just("{\"reply\": \"❌ Không tìm thấy chi nhánh '" + branchName + "'. Bạn có thể gõ 'danh sách chi nhánh' để xem tất cả.\"}");
        }

        BranchEntity branch = branchOpt.get();
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findActiveRoomTypesByBranch(branch.getId());

        if (roomTypes.isEmpty()) {
            return Mono.just("{\"reply\": \"❌ Chi nhánh " + branch.getName() + " hiện chưa có loại phòng nào.\"}");
        }

        // ✅ TẠO RESPONSE VỚI BUTTONS
        JSONObject response = new JSONObject();
        response.put("reply", String.format("Tại chi nhánh %s, chúng tôi có %d loại phòng:",
                branch.getName(), roomTypes.size()));
        response.put("type", "list_with_buttons");

        JSONArray data = new JSONArray();
        for (RoomTypeEntity rt : roomTypes) {
            JSONObject roomTypeJson = new JSONObject();
            roomTypeJson.put("name", String.format("🛏️ %s - %,.0f₫/đêm (%d người)",
                    rt.getName(), rt.getPrice(), rt.getMaxOccupancy()));
            roomTypeJson.put("url", "/customer/room-types/detail/" + rt.getId());
            data.put(roomTypeJson);
        }
        response.put("data", data);

        return Mono.just(response.toString());
    }




    public Mono<String> getGenericReply(String userMessage, String history) {
        String systemContext = buildSystemContext();
        String fullPrompt = buildPrompt(systemContext, history, userMessage);
        JSONObject requestBody = createGeminiRequest(fullPrompt);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/" + modelName + ":generateContent")
                        .queryParam("key", apiKey)
                        .build())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .onErrorResume(e -> {
                    System.err.println("❌ Gemini API Error: " + e.getMessage());
                    return Mono.just("{\"reply\": \"Xin lỗi, tôi đang gặp sự cố kết nối. Vui lòng thử lại sau.\"}");
                });
    }

    // ✅ PHẦN 4: SỬA handleCheckRoomAvailability - ƯU TIÊN CONTEXT
    private Mono<String> handleCheckRoomAvailability(String roomTypeName, String checkInStr, String checkOutStr, String context) {
        System.out.println("=== DEBUG AVAILABILITY CHECK ===");
        System.out.println("Room Type Input: '" + roomTypeName + "'");
        System.out.println("Check-in: " + checkInStr);
        System.out.println("Check-out: " + checkOutStr);

        // ✅ BƯỚC 1: Tìm chi nhánh từ CONTEXT (ưu tiên history)
        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);

        if (branchOpt.isEmpty()) {
            // Nếu không tìm thấy trong context, gợi ý user
            return Mono.just(new JSONObject()
                    .put("reply", "Bạn muốn kiểm tra phòng tại chi nhánh nào? Vui lòng nói rõ tên chi nhánh (VD: 'chi nhánh CMT8') hoặc gõ 'danh sách chi nhánh'")
                    .toString());
        }

        BranchEntity branch = branchOpt.get();
        System.out.println("✅ Found Branch from context: " + branch.getName());

        // ✅ BƯỚC 2: Clean room type name
        roomTypeName = roomTypeName.trim()
                .replaceAll("(?i)loại\\s+", "")
                .replaceAll("(?i)này", "")
                .replaceAll("(?i)chi\\s+nhánh.*", "")
                .replaceAll("(?i)tại.*", "")
                .replaceAll("(?i)ở.*", "")
                .trim();

        System.out.println("Cleaned room type name: '" + roomTypeName + "'");

        // ✅ BƯỚC 3: Tìm loại phòng LINH HOẠT
        String normalizedRoomTypeName = removeVietnameseTones(roomTypeName.toLowerCase());
        Optional<RoomTypeEntity> roomTypeOpt = Optional.empty();

        List<RoomTypeEntity> branchRoomTypes = roomTypeRepository.findActiveRoomTypesByBranch(branch.getId());

        for (RoomTypeEntity rt : branchRoomTypes) {
            String normalizedRTName = removeVietnameseTones(rt.getName().toLowerCase());

            // Match nếu:
            // - Tên giống hệt
            // - Tên chứa input
            // - Input chứa tên
            if (normalizedRTName.equals(normalizedRoomTypeName) ||
                    normalizedRTName.contains(normalizedRoomTypeName) ||
                    normalizedRoomTypeName.contains(normalizedRTName)) {
                roomTypeOpt = Optional.of(rt);
                System.out.println("✅ Matched room type: " + rt.getName());
                break;
            }
        }

        if (roomTypeOpt.isEmpty()) {
            // Suggest available room types
            String suggestions = branchRoomTypes.stream()
                    .map(RoomTypeEntity::getName)
                    .collect(Collectors.joining(", "));

            return Mono.just(new JSONObject()
                    .put("reply", String.format("❌ Không tìm thấy loại phòng '%s' tại %s. Các loại phòng có sẵn: %s",
                            roomTypeName, branch.getName(), suggestions))
                    .toString());
        }

        RoomTypeEntity roomType = roomTypeOpt.get();

        // ✅ BƯỚC 4: Parse ngày
        LocalDate checkInDate, checkOutDate;
        DateTimeFormatter[] formats = {
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("d/M/yy")
        };

        try {
            checkInStr = checkInStr.trim().replaceAll("\\s+", "/").replace("-", "/");
            checkInDate = parseFlexibleDate(checkInStr, formats);

            if (checkOutStr != null && !checkOutStr.trim().isEmpty()) {
                checkOutStr = checkOutStr.trim().replaceAll("\\s+", "/").replace("-", "/");
                checkOutDate = parseFlexibleDate(checkOutStr, formats);
            } else {
                checkOutDate = checkInDate.plusDays(1);
            }

            if (!checkOutDate.isAfter(checkInDate)) {
                checkOutDate = checkInDate.plusDays(1);
            }

        } catch (Exception e) {
            System.err.println("❌ Date parse error: " + e.getMessage());
            return Mono.just(new JSONObject()
                    .put("reply", "❌ Ngày không hợp lệ. Vui lòng nhập: DD/MM/YYYY hoặc DD/MM")
                    .toString());
        }

        // ✅ BƯỚC 5: Kiểm tra phòng trống
        long totalAvailableRooms = roomRepository.countByRoomTypeIdAndStatus(
                roomType.getId(),
                RoomStatus.AVAILABLE
        );

        if (totalAvailableRooms == 0) {
            return Mono.just(new JSONObject()
                    .put("reply", "❌ Loại phòng '" + roomType.getName() + "' tại " + branch.getName() +
                            " hiện không có phòng khả dụng (đang bảo trì).")
                    .toString());
        }

        long bookedRooms = roomRepository.countBookedAvailableRoomsByRoomTypeAndDateRange(
                roomType.getId(),
                checkInDate,
                checkOutDate
        );

        long availableRooms = totalAvailableRooms - bookedRooms;

        System.out.println("Total: " + totalAvailableRooms + ", Booked: " + bookedRooms + ", Available: " + availableRooms);

        // ✅ BƯỚC 6: Tạo response
        DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        JSONObject response = new JSONObject();

        if (availableRooms > 0) {
            // ✅ THÊM: Tạo draft ngay khi có phòng trống
            ChatbotBookingDraftEntity draft = ChatbotBookingDraftEntity.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .roomType(roomType)
                    .branch(branch)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .numberOfRooms(1)  // Mặc định 1 phòng
                    .adults(2)         // Mặc định 2 người lớn
                    .children(0)
                    .currentStep(BookingDraftStep.ROOM_SELECTED)
                    .includeBreakfast(false)
                    .includeSpa(false)
                    .includeAirportTransfer(false)
                    .build();

            draft = draftRepository.save(draft);

            System.out.println("✅ Created draft: " + draft.getDraftCode());

            response.put("reply", String.format(
                    "✅ Tin vui! Còn %d phòng '%s' trống tại %s từ %s đến %s.\n\n" +
                            "💡 Bạn muốn đặt phòng ngay không? Tôi sẽ giúp bạn hoàn tất trong vài bước đơn giản!",
                    availableRooms,
                    roomType.getName(),
                    branch.getName(),
                    checkInDate.format(displayFormat),
                    checkOutDate.format(displayFormat)
            ));

            response.put("type", "list_with_buttons");
            response.put("draftCode", draft.getDraftCode()); // ✅ THÊM: Để tracking draft

            JSONArray buttons = new JSONArray();

            // ✅ SỬA: Button đặt phòng ngay qua chatbot
            JSONObject bookBtn = new JSONObject();
            bookBtn.put("name", "🎫 Đặt phòng qua Chat");
            bookBtn.put("action", "start_booking:" + draft.getDraftCode()); // ✅ THÊM: action trigger
            buttons.put(bookBtn);

            JSONObject detailBtn = new JSONObject();
            detailBtn.put("name", "📋 Xem chi tiết");
            detailBtn.put("url", "/customer/room-types/detail/" + roomType.getId());
            buttons.put(detailBtn);

            response.put("data", buttons);

        }else {
            response.put("reply", String.format(
                    "😔 Rất tiếc, loại phòng '%s' tại %s đã hết phòng từ %s đến %s.",
                    roomType.getName(),
                    branch.getName(),
                    checkInDate.format(displayFormat),
                    checkOutDate.format(displayFormat)
            ));
            response.put("type", "list_with_buttons");

            JSONArray data = new JSONArray();
            JSONObject viewOthersBtn = new JSONObject();
            viewOthersBtn.put("name", "🔍 Xem loại phòng khác");
            viewOthersBtn.put("url", "/customer/branches/detail/" + branch.getId());
            data.put(viewOthersBtn);

            response.put("data", data);
        }

        return Mono.just(response.toString());
    }

    // ✅ PHẦN 5: Helper methods
    private LocalDate parseFlexibleDate(String dateStr, DateTimeFormatter[] formats) {
        if (dateStr.matches("\\d{1,2}/\\d{1,2}")) {
            dateStr = dateStr + "/" + LocalDate.now().getYear();
        } else if (dateStr.matches("\\d{1,2}/\\d{1,2}/\\d{2}")) {
            dateStr = dateStr.replaceAll("/(\\d{2})$", "/20$1");
        }

        for (DateTimeFormatter fmt : formats) {
            try {
                LocalDate date = LocalDate.parse(dateStr, fmt);
                if (date.isBefore(LocalDate.now())) {
                    date = date.plusYears(1);
                }
                return date;
            } catch (DateTimeParseException ignored) {}
        }

        throw new IllegalArgumentException("Invalid date format: " + dateStr);
    }

    /**
     * Thử parse nhiều dạng ngày khác nhau
     */
    private LocalDate tryParseDate(String input, DateTimeFormatter[] formats) {
        for (DateTimeFormatter fmt : formats) {
            try {
                LocalDate date = LocalDate.parse(input, fmt);
                // Nếu format chỉ có ngày/tháng thì thêm năm hiện tại
                if (!input.matches(".*\\d{4}")) {
                    date = date.withYear(LocalDate.now().getYear());
                }
                return date;
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Invalid date format: " + input);
    }

    /**
     * Xóa dấu tiếng Việt để so sánh gần đúng tên phòng
     */
    private String removeVietnameseTones(String str) {
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replaceAll("đ", "d").replaceAll("Đ", "D");
    }

    private String buildSystemContext() {
        StringBuilder context = new StringBuilder();
        try {
            List<BranchEntity> branches = branchRepository.findByStatus(BranchStatus.ACTIVE);
            context.append("=== CHI NHÁNH ===\n");
            branches.forEach(branch -> context.append(String.format("- %s: %s, %s. SĐT: %s\n", branch.getName(), branch.getAddress(), branch.getProvince(), branch.getPhoneNumber())));

            List<RoomTypeEntity> roomTypes = roomTypeRepository.findByStatus(Status.ACTIVE);
            context.append("\n=== LOẠI PHÒNG ===\n");
            roomTypes.stream().limit(10).forEach(rt -> context.append(String.format("- %s tại %s: %,.0f VNĐ/đêm, %d người, giường %s\n", rt.getName(), rt.getBranch().getName(), rt.getPrice(), rt.getMaxOccupancy(), rt.getBedType())));

            List<MenuItemEntity> menuItems = menuItemRepository.findByStatusAndIsAvailable(Status.ACTIVE, true);
            context.append("\n=== MÓN ĂN ===\n");
            menuItems.stream().limit(15).forEach(item -> context.append(String.format("- %s: %,.0f VNĐ\n", item.getName(), item.getPrice())));

            // ===== THÊM: DANH MỤC MÓN ĂN =====
            context.append("\n=== DANH MỤC MÓN ĂN ===\n");
            for (BranchEntity branch : branches) {
                List<MenuCategoryEntity> categories = menuCategoryRepository
                        .findByBranchIdAndStatus(branch.getId(), Status.ACTIVE);

                if (!categories.isEmpty()) {
                    context.append(String.format("Tại %s:\n", branch.getName()));
                    categories.forEach(cat -> {
                        long itemCount = menuItemRepository.countByCategoryIdAndStatus(
                                cat.getId(), Status.ACTIVE
                        );
                        context.append(String.format(
                                "  • %s (%d món)\n",
                                cat.getName(),
                                itemCount
                        ));
                    });
                }
            }

            // ===== THÊM: MÓN ĂN PHỔ BIẾN =====
            context.append("\n=== MÓN ĂN PHỔ BIẾN ===\n");
            for (BranchEntity branch : branches) {
                List<MenuItemEntity> popularItems = menuItemRepository
                        .findAvailableItemsByBranch(branch.getId(), Status.ACTIVE)
                        .stream()
                        .limit(5) // Lấy 5 món đầu tiên
                        .collect(Collectors.toList());

                if (!popularItems.isEmpty()) {
                    context.append(String.format("Tại %s:\n", branch.getName()));
                    popularItems.forEach(item -> context.append(String.format(
                            "  • %s: %,.0f VNĐ - %s\n",
                            item.getName(),
                            item.getPrice(),
                            item.getCategory().getName()
                    )));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error building context: " + e.getMessage());
        }
        return context.toString();
    }

//    private String buildPrompt(String systemContext, String history, String userMessage) {
//        return String.format("""
//                BẠN LÀ CHATBOT TƯ VẤN CỦA HỆ THỐNG KHÁCH SẠN & NHÀ HÀNG DIEK.
//                Nhiệm vụ: Trả lời ngắn gọn, thân thiện, chuyên nghiệp bằng tiếng Việt. Luôn dựa vào dữ liệu hệ thống và lịch sử chat để trả lời. Nếu không biết, hãy nói "Tôi chưa có thông tin này, bạn vui lòng liên hệ SĐT: 0324245325 để được hỗ trợ nhé!".
//
//                DỮ LIỆU HỆ THỐNG:
//                %s
//
//                LỊCH SỬ TRÒ CHUYỆN GẦN ĐÂY:
//                %s
//
//                CÂU HỎI MỚI CỦA KHÁCH:
//                %s
//
//                TRẢ LỜI:
//                """, systemContext, history != null ? history : "Không có", userMessage);
//    }

//    // ✅ PHẦN 6: buildPrompt - CẢI THIỆN
//    private String buildPrompt(String systemContext, String history, String userMessage) {
//        return String.format("""
//        BẠN LÀ CHATBOT HỖ TRỢ KHÁCH SẠN DIEK.
//
//        QUY TẮC:
//        1. Trả lời ngắn gọn, thân thiện
//        2. Khi khách hỏi về chi nhánh → Gợi ý: "Gõ 'danh sách chi nhánh' để xem tất cả"
//        3. Khi khách đề cập địa điểm (Cần Thơ, TPHCM) → Đọc DỮ LIỆU và trả lời chi nhánh tại đó
//        4. QUAN TRỌNG: Nếu trong LỊCH SỬ đã nhắc đến chi nhánh, HÃY NHỚ chi nhánh đó cho câu hỏi tiếp theo
//        5. Không biết → "Vui lòng liên hệ: 0324245325"
//
//        DỮ LIỆU:
//        %s
//
//        LỊCH SỬ (GHI NHỚ CHI NHÁNH TRONG NÀY):
//        %s
//
//        CÂU HỎI:
//        %s
//
//        TRẢ LỜI:
//        """, systemContext, history != null ? history : "(Chưa có)", userMessage);
//    }

//    // ✅ SỬA buildPrompt() trong ChatbotService.java
//
//    private String buildPrompt(String systemContext, String history, String userMessage) {
//        return String.format("""
//        BẠN LÀ CHATBOT HỖ TRỢ KHÁCH SẠN & NHÀ HÀNG DIEK.
//
//        QUY TẮC:
//        1. Trả lời ngắn gọn, thân thiện, chuyên nghiệp
//        2. Ưu tiên sử dụng thông tin từ DỮ LIỆU HỆ THỐNG
//        3. NHỚ chi nhánh được đề cập trong LỊCH SỬ để trả lời câu hỏi tiếp theo
//
//        ===== HƯỚNG DẪN THEO TÌNH HUỐNG =====
//
//        🏨 **CHI NHÁNH & PHÒNG:**
//        - Hỏi về chi nhánh → Gợi ý: "Gõ 'danh sách chi nhánh'"
//        - Hỏi về loại phòng → Gợi ý: "Loại phòng tại [tên chi nhánh]"
//        - Kiểm tra phòng trống → Cần: loại phòng + ngày + chi nhánh
//
//        🍽️ **MÓN ĂN & ĐẶT MÓN:**
//        - Hỏi về món ăn → Gợi ý: "Xem thực đơn tại [chi nhánh]"
//        - Hỏi về danh mục → Liệt kê các danh mục có sẵn
//        - Tìm món cụ thể → Tìm trong danh mục phù hợp
//        - Thêm món → YÊU CẦU ĐĂNG NHẬP, sau đó: "Thêm [tên món] x [số lượng]"
//        - Xem giỏ hàng → "Xem giỏ hàng" (cần đăng nhập)
//        - Đặt món → "Đặt món" hoặc "Thanh toán" (cần đăng nhập + có món trong giỏ)
//
//        ⚠️ **LƯU Ý QUAN TRỌNG:**
//        - Các tính năng GIỎ HÀNG và ĐẶT MÓN cần đăng nhập
//        - Nếu user chưa đăng nhập, hướng dẫn đăng nhập trước
//        - Luôn xác nhận chi nhánh trước khi xem món hoặc đặt phòng
//
//        📌 **KHI KHÔNG BIẾT:**
//        "Vui lòng liên hệ: 0324245325"
//
//        DỮ LIỆU HỆ THỐNG:
//        %s
//
//        LỊCH SỬ CHAT (NHỚ CHI NHÁNH TRONG NÀY):
//        %s
//
//        CÂU HỎI MỚI:
//        %s
//
//        TRẢ LỜI (ngắn gọn, rõ ràng):
//        """, systemContext, history != null ? history : "(Chưa có)", userMessage);
//    }
    /**
     * ===== CẬP NHẬT: buildPrompt - Hướng dẫn rõ ràng hơn =====
     */
    private String buildPrompt(String systemContext, String history, String userMessage) {
        return String.format("""
        BẠN LÀ CHATBOT HỖ TRỢ KHÁCH SẠN & NHÀ HÀNG DIEK.
        
        QUY TẮC:
        1. Trả lời ngắn gọn, thân thiện
        2. Luôn xác định chi nhánh trước khi xử lý request
        3. NHỚ chi nhánh trong lịch sử chat
        
        ===== HƯỚNG DẪN THEO TÌNH HUỐNG =====
        
        🏨 **CHI NHÁNH & PHÒNG:**
        - "danh sách chi nhánh" → Liệt kê chi nhánh
        - "loại phòng tại X" → Liệt kê loại phòng ở chi nhánh X
        - "phòng X từ DD/MM đến DD/MM" → Kiểm tra phòng trống
        - "đặt phòng" → Bắt đầu quy trình đặt phòng
        
        🍽️ **MÓN ĂN & ĐẶT MÓN:**
        - "xem thực đơn" hoặc "xem món ăn" → YÊU CẦU CHI NHÁNH
        - "thêm [tên món]" → Hỏi: Thêm giỏ hay Đặt ngay?
        - "xem giỏ hàng" → Đưa link giỏ hàng
        - "đặt món" hoặc "thanh toán" → Đưa link checkout
        
        ⚠️ **QUAN TRỌNG:**
        - Các tính năng giỏ hàng/đặt món CẦN ĐĂNG NHẬP
        - Nếu user chưa đăng nhập → Hướng dẫn đăng nhập qua nút
        - KHÔNG BẮT BUỘC user phải qua giỏ hàng, cho phép đặt ngay
        
        📌 **KHI KHÔNG BIẾT:**
        "Vui lòng liên hệ: 0324245325"
        
        DỮ LIỆU HỆ THỐNG:
        %s
        
        LỊCH SỬ CHAT (30 phút gần nhất):
        %s
        
        CÂU HỎI MỚI:
        %s
        
        TRẢ LỜI (ngắn gọn, rõ ràng):
        """, systemContext, history != null ? history : "(Chưa có)", userMessage);
    }

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
        return requestBody;
    }

    private String parseResponse(String responseBody) {
        JSONObject responseJson = new JSONObject();
        try {
            JSONObject json = new JSONObject(responseBody);
            if (!json.has("candidates") || json.getJSONArray("candidates").isEmpty()) {
                responseJson.put("reply", "Xin lỗi, tôi không thể tạo câu trả lời phù hợp lúc này.");
                return responseJson.toString();
            }
            JSONObject candidate = json.getJSONArray("candidates").getJSONObject(0);
            if (candidate.has("finishReason") && !candidate.getString("finishReason").equals("STOP")) {
                responseJson.put("reply", "Xin lỗi, câu trả lời bị chặn do chính sách an toàn.");
                return responseJson.toString();
            }
            String text = candidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
            responseJson.put("reply", text.isEmpty() ? "Xin lỗi, câu trả lời trống." : text);
            return responseJson.toString();
        } catch (Exception e) {
            System.err.println("❌ Parse error: " + e.getMessage() + " | Body: " + responseBody);
            responseJson.put("reply", "Xin lỗi, tôi không thể xử lý câu trả lời lúc này.");
            return responseJson.toString();
        }
    }


//    này laf chat hổ trợ đặt phòng
// ✅ THÊM: Thêm suggestion đặt phòng vào response availability
private Mono<String> addBookingSuggestion(String originalResponse,
                                          String roomTypeName,
                                          String checkIn,
                                          String checkOut) {
    try {
        JSONObject jsonResponse = new JSONObject(originalResponse);
        String reply = jsonResponse.getString("reply");

        // Thêm câu gợi ý
        reply += "\n\n💡 Bạn có muốn đặt phòng ngay không? Chỉ cần nói 'đặt phòng' là tôi sẽ hỗ trợ bạn!";

        jsonResponse.put("reply", reply);

        // Thêm quick reply buttons
        JSONArray quickReplies = new JSONArray();
        quickReplies.put(new JSONObject()
                .put("type", "quick_reply")
                .put("text", "🎫 Đặt phòng ngay")
                .put("payload", "BOOK_NOW"));
        quickReplies.put(new JSONObject()
                .put("type", "quick_reply")
                .put("text", "📋 Xem chi tiết")
                .put("payload", "VIEW_DETAIL"));

        jsonResponse.put("quickReplies", quickReplies);

        return Mono.just(jsonResponse.toString());
    } catch (Exception e) {
        return Mono.just(originalResponse);
    }
}

    // ✅ THÊM: Handle booking intent
    private Mono<String> handleBookingIntent(String userMessage, String context) {
        // Tìm thông tin phòng từ context
        Optional<RoomTypeEntity> roomTypeOpt = extractRoomTypeFromContext(context);
        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);
        LocalDate[] dates = extractDatesFromContext(context);

        if (roomTypeOpt.isEmpty() || dates == null) {
            return Mono.just(new JSONObject()
                    .put("reply", "Để đặt phòng, tôi cần biết:\n" +
                            "1️⃣ Loại phòng bạn muốn đặt\n" +
                            "2️⃣ Ngày nhận phòng và trả phòng\n" +
                            "3️⃣ Chi nhánh (nếu có)\n\n" +
                            "Ví dụ: 'Tôi muốn đặt phòng Deluxe tại CMT8 từ 25/12 đến 27/12'")
                    .toString());
        }

        // Tạo booking draft
        ChatbotBookingDraftEntity draft = ChatbotBookingDraftEntity.builder()
                .sessionId(UUID.randomUUID().toString())
                .roomType(roomTypeOpt.get())
                .branch(branchOpt.orElse(roomTypeOpt.get().getBranch()))
                .checkInDate(dates[0])
                .checkOutDate(dates[1])
                .numberOfRooms(1)
                .adults(2)
                .children(0)
                .currentStep(BookingDraftStep.ROOM_SELECTED)
                .build();

        draft = draftRepository.save(draft);

        return Mono.just(createInfoCollectionResponse(draft));
    }

//    private Mono<String> handleBookingProcess(String userMessage,
//                                              ChatbotBookingDraftEntity draft,
//                                              String context) {
//        System.out.println("=== BOOKING PROCESS ===");
//        System.out.println("Current step: " + draft.getCurrentStep());
//        System.out.println("User message: " + userMessage);
//
//        // ✅ THÊM: Xử lý theo từng bước
//        switch (draft.getCurrentStep()) {
//            case ROOM_SELECTED:
//                // Bắt đầu thu thập thông tin
//                return Mono.just(createInfoCollectionResponse(draft));
//
//            case INFO_COLLECTING: // ✅ THÊM enum này vào BookingDraftStep
//                // Đang thu thập thông tin
//                return handleInfoCollection(userMessage, draft);
//
//            case INFO_COLLECTED:
//                // Hỏi về dịch vụ
//                return Mono.just(createServiceSelectionResponse(draft));
//
//            case SERVICES_SELECTED:
//                // Sẵn sàng thanh toán
//                return Mono.just(createPaymentReadyResponse(draft));
//
//            default:
//                return Mono.just(new JSONObject()
//                        .put("reply", "Có lỗi xảy ra. Vui lòng thử lại!")
//                        .toString());
//        }
//    }

    private Mono<String> handleBookingProcess(String userMessage,
                                              ChatbotBookingDraftEntity draft,
                                              String context) {
        System.out.println("=== BOOKING PROCESS ===");
        System.out.println("Current step: " + draft.getCurrentStep());
        System.out.println("User message: " + userMessage);

        // ✅ SỬA: Xử lý theo từng bước
        switch (draft.getCurrentStep()) {
            case ROOM_SELECTED:
                // ✅ SỬA: Không return ngay, mà gọi handleInfoCollection để extract
                return handleInfoCollection(userMessage, draft);

            case INFO_COLLECTING:
                // Đang thu thập thông tin, tiếp tục extract
                return handleInfoCollection(userMessage, draft);

            case INFO_COLLECTED:
                // Đã có đủ thông tin, hỏi về dịch vụ
                return Mono.just(createServiceSelectionResponse(draft));

            case SERVICES_SELECTING:
                // Đang chọn dịch vụ
                return handleServiceSelection(userMessage, draft);

            case SERVICES_SELECTED:
            case READY_TO_PAY:
                // Sẵn sàng thanh toán
                return Mono.just(createPaymentReadyResponse(draft));

            default:
                return Mono.just(new JSONObject()
                        .put("reply", "Có lỗi xảy ra. Vui lòng thử lại!")
                        .toString());
        }
    }

    // ✅ THÊM: Tìm draft gần nhất từ context/history
    private Optional<ChatbotBookingDraftEntity> findRecentDraftFromContext(String context) {
        // Tìm tất cả draft codes trong context
        Pattern draftPattern = Pattern.compile("DRAFT\\d+");
        Matcher matcher = draftPattern.matcher(context);

        String lastDraftCode = null;
        while (matcher.find()) {
            lastDraftCode = matcher.group();
        }

        if (lastDraftCode != null) {
            Optional<ChatbotBookingDraftEntity> draft = draftRepository.findByDraftCode(lastDraftCode);
            if (draft.isPresent() && draft.get().getExpiresAt().isAfter(LocalDateTime.now())) {
                return draft;
            }
        }

        return Optional.empty();
    }


    // ✅ THÊM vào ChatbotService

    /**
     * Tạo response bắt đầu thu thập thông tin khách hàng
     */
    private String createInfoCollectionResponse(ChatbotBookingDraftEntity draft) {
        JSONObject response = new JSONObject();

        // Tính toán số đêm
        int nights = (int) ChronoUnit.DAYS.between(
                draft.getCheckInDate(),
                draft.getCheckOutDate()
        );

        // Format ngày
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String checkIn = draft.getCheckInDate().format(formatter);
        String checkOut = draft.getCheckOutDate().format(formatter);

        // Tạo message
        StringBuilder message = new StringBuilder();
        message.append("✅ Tuyệt vời! Tôi sẽ giúp bạn đặt phòng.\n\n");
        message.append("📋 **Thông tin đã chọn:**\n");
        message.append(String.format("🏨 Chi nhánh: %s\n", draft.getBranch().getName()));
        message.append(String.format("🛏️ Loại phòng: %s\n", draft.getRoomType().getName()));
        message.append(String.format("📅 Thời gian: %s → %s (%d đêm)\n", checkIn, checkOut, nights));
        message.append(String.format("👥 Số khách: %d người lớn", draft.getAdults()));

        if (draft.getChildren() > 0) {
            message.append(String.format(", %d trẻ em", draft.getChildren()));
        }
        message.append("\n\n");

        message.append("📝 **Để hoàn tất đặt phòng, tôi cần:**\n");
        message.append("1️⃣ Họ và tên của bạn\n");
        message.append("2️⃣ Email (để nhận xác nhận)\n");
        message.append("3️⃣ Số điện thoại liên hệ\n\n");

        message.append("Bạn có thể gửi thông tin theo mẫu:\n");
        message.append("```\n");
        message.append("Tên: Nguyễn Văn A\n");
        message.append("Email: example@email.com\n");
        message.append("SĐT: 0912345678\n");
        message.append("```\n\n");
        message.append("Hoặc gửi từng thông tin riêng lẻ cũng được nhé! 😊");

        response.put("reply", message.toString());
        response.put("draftCode", draft.getDraftCode());
        response.put("type", "info_collection");

        return response.toString();
    }

    private String createNextInfoRequest(ChatbotBookingDraftEntity draft) {
        JSONObject response = new JSONObject();
        StringBuilder message = new StringBuilder();

        message.append("📋 **Thông tin đã có:**\n");

        if (draft.getGuestName() != null) {
            message.append("✅ Tên: ").append(draft.getGuestName()).append("\n");
        } else {
            message.append("⏳ Tên: Chưa có\n");
        }

        if (draft.getGuestEmail() != null) {
            message.append("✅ Email: ").append(draft.getGuestEmail()).append("\n");
        } else {
            message.append("⏳ Email: Chưa có\n");
        }

        if (draft.getGuestPhone() != null) {
            message.append("✅ SĐT: ").append(draft.getGuestPhone()).append("\n");
        } else {
            message.append("⏳ SĐT: Chưa có\n");
        }

        message.append("\n❓ **Cần thêm:**\n");

        if (draft.getGuestName() == null) {
            message.append("• Họ tên của bạn (VD: Nguyễn Văn A)\n");
        }
        if (draft.getGuestEmail() == null) {
            message.append("• Email (VD: example@email.com)\n");
        }
        if (draft.getGuestPhone() == null) {
            message.append("• Số điện thoại (VD: 0912345678)\n");
        }

        message.append("\n💬 Bạn có thể gửi tất cả cùng lúc hoặc từng thông tin riêng lẻ nhé!");

        response.put("reply", message.toString());
        response.put("draftCode", draft.getDraftCode());
        response.put("type", "info_collection");

        return response.toString();
    }

    private Mono<String> handleInfoCollection(String userMessage, ChatbotBookingDraftEntity draft) {
        System.out.println("=== EXTRACTING INFO ===");
        System.out.println("Message: " + userMessage);
        System.out.println("Current info - Name: " + draft.getGuestName() +
                ", Email: " + draft.getGuestEmail() +
                ", Phone: " + draft.getGuestPhone());

        boolean updated = false;
        String normalizedMessage = userMessage.trim();

        // ✅ Thu thập tên
        if (draft.getGuestName() == null) {
            // Pattern: "Tên: X" hoặc "Tên X" hoặc chỉ tên riêng
            Pattern namePattern = Pattern.compile(
                    "(?i)(?:tên\\s*:?\\s*)?([\\p{L}\\s]{2,50})(?=\\s*(?:email|sđt|$))",
                    Pattern.UNICODE_CHARACTER_CLASS
            );
            Matcher nameMatcher = namePattern.matcher(normalizedMessage);

            if (nameMatcher.find()) {
                String name = nameMatcher.group(1).trim()
                        .replaceAll("(?i)\\s*email.*", "")
                        .replaceAll("(?i)\\s*sđt.*", "")
                        .replaceAll("(?i)\\s*số điện thoại.*", "")
                        .trim();

                // Validate: phải có ít nhất 2 ký tự chữ
                if (name.length() >= 2 && name.matches(".*[\\p{L}]{2,}.*")) {
                    draft.setGuestName(name);
                    updated = true;
                    System.out.println("✅ Extracted name: '" + name + "'");
                }
            }
        }

        // ✅ Thu thập email
        if (draft.getGuestEmail() == null) {
            Pattern emailPattern = Pattern.compile(
                    "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
            );
            Matcher emailMatcher = emailPattern.matcher(normalizedMessage);

            if (emailMatcher.find()) {
                String email = emailMatcher.group(1).toLowerCase().trim();
                draft.setGuestEmail(email);
                updated = true;
                System.out.println("✅ Extracted email: '" + email + "'");
            }
        }

        // ✅ Thu thập phone
        if (draft.getGuestPhone() == null) {
            Pattern phonePattern = Pattern.compile(
                    "(?:sđt|số điện thoại|phone)?\\s*:?\\s*(0\\d{9,10})"
            );
            Matcher phoneMatcher = phonePattern.matcher(normalizedMessage);

            if (phoneMatcher.find()) {
                String phone = phoneMatcher.group(1).trim();
                draft.setGuestPhone(phone);
                updated = true;
                System.out.println("✅ Extracted phone: '" + phone + "'");
            }
        }

        // ✅ Lưu draft nếu có update
        if (updated) {
            draft.setCurrentStep(BookingDraftStep.INFO_COLLECTING);
            draft = draftRepository.save(draft);

            System.out.println("💾 Saved draft with updated info");
        }

        // ✅ Check đầy đủ thông tin
        if (isInfoComplete(draft)) {
            draft.setCurrentStep(BookingDraftStep.INFO_COLLECTED);
            draft = draftRepository.save(draft);

            System.out.println("✅ All info collected! Moving to service selection");
            return Mono.just(createServiceSelectionResponse(draft));
        } else {
            // ✅ Chưa đủ thông tin
            System.out.println("⏳ Still missing info, requesting more");

            // ✅ QUAN TRỌNG: Nếu lần đầu (ROOM_SELECTED), show full request
            if (draft.getCurrentStep() == BookingDraftStep.ROOM_SELECTED) {
                draft.setCurrentStep(BookingDraftStep.INFO_COLLECTING);
                draft = draftRepository.save(draft);
                return Mono.just(createInfoCollectionResponse(draft));
            } else {
                // Đã thu thập một phần, show progress
                return Mono.just(createNextInfoRequest(draft));
            }
        }
    }

    // ✅ THÊM: Method xử lý chọn dịch vụ
    private Mono<String> handleServiceSelection(String userMessage, ChatbotBookingDraftEntity draft) {
        System.out.println("=== SELECTING SERVICES ===");
        System.out.println("Message: " + userMessage);

        String normalized = userMessage.toLowerCase().trim();

        // Check nếu user muốn thêm dịch vụ
        if (normalized.matches(".*(có|thêm|muốn|cần|yes|ok|được).*")) {
            // Hỏi chi tiết dịch vụ nào
            JSONObject response = new JSONObject();
            response.put("reply",
                    "Bạn muốn thêm dịch vụ nào?\n\n" +
                            "Trả lời theo format:\n" +
                            "- Buffet sáng: có/không\n" +
                            "- Spa: có/không\n" +
                            "- Đưa đón sân bay: có/không\n\n" +
                            "Hoặc gửi: 'có tất cả' / 'không cần dịch vụ'"
            );
            response.put("draftCode", draft.getDraftCode());
            return Mono.just(response.toString());
        }
        // Check nếu user không muốn dịch vụ
        else if (normalized.matches(".*(không|no|bỏ qua|skip|thôi).*")) {
            draft.setIncludeBreakfast(false);
            draft.setIncludeSpa(false);
            draft.setIncludeAirportTransfer(false);
            draft.setCurrentStep(BookingDraftStep.SERVICES_SELECTED);
            draft = draftRepository.save(draft);

            return Mono.just(createPaymentReadyResponse(draft));
        }
        // Parse dịch vụ cụ thể
        else {
            boolean updated = false;

            if (normalized.contains("buffet") || normalized.contains("sáng")) {
                draft.setIncludeBreakfast(true);
                updated = true;
            }
            if (normalized.contains("spa")) {
                draft.setIncludeSpa(true);
                updated = true;
            }
            if (normalized.contains("sân bay") || normalized.contains("đưa đón")) {
                draft.setIncludeAirportTransfer(true);
                updated = true;
            }

            if (updated) {
                draft.setCurrentStep(BookingDraftStep.SERVICES_SELECTED);
                draft = draftRepository.save(draft);
                return Mono.just(createPaymentReadyResponse(draft));
            }

            // Không hiểu, hỏi lại
            return Mono.just(new JSONObject()
                    .put("reply", "Xin lỗi, tôi không hiểu. Bạn có muốn thêm dịch vụ không? (có/không)")
                    .put("draftCode", draft.getDraftCode())
                    .toString());
        }
    }

    // ✅ THÊM VÀO ChatbotService.java - SAU CÁC HANDLER CỦA PHÒNG

// ===== PHẦN 1: XEM DANH SÁCH MÓN ĂN =====

    /**
     * Xem danh sách món ăn tại chi nhánh
     */
    private Mono<String> handleGetMenuList(String context) {
        System.out.println("=== GETTING MENU LIST ===");

        // Tìm chi nhánh từ context
        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);

        if (branchOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", "Bạn muốn xem thực đơn tại chi nhánh nào? " +
                            "Vui lòng cho tôi biết tên chi nhánh (VD: 'xem món ăn tại CMT8')")
                    .toString());
        }

        BranchEntity branch = branchOpt.get();

        // Lấy tất cả danh mục có món ăn active
        List<MenuCategoryEntity> categories = menuCategoryRepository
                .findByBranchIdAndStatus(branch.getId(), Status.ACTIVE);

        if (categories.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", String.format("Chi nhánh %s chưa có thực đơn.", branch.getName()))
                    .toString());
        }

        JSONObject response = new JSONObject();
        response.put("reply", String.format(
                "🍽️ Thực đơn tại %s gồm %d danh mục:",
                branch.getName(), categories.size()
        ));
        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();
        for (MenuCategoryEntity category : categories) {
            long itemCount = menuItemRepository.countByCategoryIdAndStatus(
                    category.getId(), Status.ACTIVE
            );

            if (itemCount > 0) {
                JSONObject btn = new JSONObject();
                btn.put("name", String.format("🍴 %s (%d món)", category.getName(), itemCount));
                btn.put("action", "view_category:" + category.getId());
                buttons.put(btn);
            }
        }

        response.put("data", buttons);
        return Mono.just(response.toString());
    }

// ===== PHẦN 2: XEM MÓN THEO DANH MỤC =====

    /**
     * Xem món ăn theo danh mục
     */
    private Mono<String> handleMenuByCategory(String categoryName, String context) {
        System.out.println("=== MENU BY CATEGORY: " + categoryName);

        // Tìm chi nhánh từ context
        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);

        if (branchOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", "Vui lòng cho tôi biết bạn đang quan tâm đến chi nhánh nào?")
                    .toString());
        }

        BranchEntity branch = branchOpt.get();
        String normalizedCategory = removeVietnameseTones(categoryName.toLowerCase());

        // Tìm category
        Optional<MenuCategoryEntity> categoryOpt = menuCategoryRepository
                .findByBranchIdAndStatus(branch.getId(), Status.ACTIVE)
                .stream()
                .filter(cat -> removeVietnameseTones(cat.getName().toLowerCase())
                        .contains(normalizedCategory))
                .findFirst();

        if (categoryOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", String.format(
                            "Không tìm thấy danh mục '%s' tại %s. " +
                                    "Gõ 'xem thực đơn' để xem tất cả danh mục.",
                            categoryName, branch.getName()
                    ))
                    .toString());
        }

        MenuCategoryEntity category = categoryOpt.get();

        // Lấy món ăn trong category
        List<MenuItemEntity> menuItems = menuItemRepository
                .findByCategoryIdAndStatusAndIsAvailable(
                        category.getId(), Status.ACTIVE, true
                );

        if (menuItems.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", String.format(
                            "Danh mục %s hiện chưa có món nào.",
                            category.getName()
                    ))
                    .toString());
        }

        JSONObject response = new JSONObject();
        response.put("reply", String.format(
                "🍽️ Danh mục **%s** tại %s có %d món:",
                category.getName(), branch.getName(), menuItems.size()
        ));
        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();
        for (MenuItemEntity item : menuItems) {
            JSONObject btn = new JSONObject();
            btn.put("name", String.format(
                    "%s - %,d₫",
                    item.getName(),
                    item.getPrice().longValue()
            ));
            btn.put("url", "/menu-items/" + item.getId());
            buttons.put(btn);
        }

        response.put("data", buttons);

        // Thêm gợi ý thêm vào giỏ
        response.put("suggestion", "💡 Gõ 'thêm [tên món]' để thêm vào giỏ hàng!");

        return Mono.just(response.toString());
    }

// ===== PHẦN 3: TÌM KIẾM MÓN ĂN =====

    /**
     * Tìm kiếm món ăn theo keyword
     */
    private Mono<String> handleSearchMenu(String keyword, String context) {
        System.out.println("=== SEARCH MENU: " + keyword);

        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);

        if (branchOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", "Bạn muốn tìm món tại chi nhánh nào?")
                    .toString());
        }

        BranchEntity branch = branchOpt.get();
        String normalizedKeyword = removeVietnameseTones(keyword.toLowerCase());

        // Tìm món trong các category của chi nhánh
        List<MenuCategoryEntity> categories = menuCategoryRepository
                .findByBranchIdAndStatus(branch.getId(), Status.ACTIVE);

        List<MenuItemEntity> foundItems = new ArrayList<>();

        for (MenuCategoryEntity category : categories) {
            List<MenuItemEntity> items = menuItemRepository
                    .findByCategoryIdAndStatusAndIsAvailable(
                            category.getId(), Status.ACTIVE, true
                    );

            items.stream()
                    .filter(item -> {
                        String normalizedName = removeVietnameseTones(
                                item.getName().toLowerCase()
                        );
                        return normalizedName.contains(normalizedKeyword);
                    })
                    .forEach(foundItems::add);
        }

        if (foundItems.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", String.format(
                            "Không tìm thấy món '%s' tại %s. " +
                                    "Gõ 'xem thực đơn' để xem tất cả món.",
                            keyword, branch.getName()
                    ))
                    .toString());
        }

        JSONObject response = new JSONObject();
        response.put("reply", String.format(
                "🔍 Tìm thấy %d món phù hợp với '%s':",
                foundItems.size(), keyword
        ));
        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();
        for (MenuItemEntity item : foundItems.subList(0, Math.min(5, foundItems.size()))) {
            JSONObject btn = new JSONObject();
            btn.put("name", String.format(
                    "%s - %,d₫",
                    item.getName(),
                    item.getPrice().longValue()
            ));
            btn.put("url", "/menu-items/" + item.getId());
            buttons.put(btn);
        }

        response.put("data", buttons);
        response.put("suggestion", "💡 Gõ 'thêm [tên món] x [số lượng]' để thêm vào giỏ!");

        return Mono.just(response.toString());
    }

// ===== PHẦN 4: THÊM VÀO GIỎ HÀNG =====

//    /**
//     * Thêm món vào giỏ hàng qua chat
//     * Format: "thêm [tên món] x [số lượng]"
//     */
//    private Mono<String> handleAddToCart(String itemName, String message, String context) {
//        System.out.println("=== ADD TO CART: " + itemName);
//
//        // ✅ KIỂM TRA ĐĂNG NHẬP (quan trọng!)
//        // Note: Cần inject thêm AuthService và HttpSession vào constructor
//        // Tạm thời return message yêu cầu đăng nhập
//        return Mono.just(new JSONObject()
//                .put("reply",
//                        "⚠️ Để thêm món vào giỏ hàng, bạn cần đăng nhập.\n\n" +
//                                "Vui lòng:\n" +
//                                "1️⃣ Đăng nhập vào hệ thống\n" +
//                                "2️⃣ Quay lại chat và thử lại\n\n" +
//                                "Hoặc bạn có thể xem món và thêm trực tiếp từ trang web!")
//                .put("type", "list_with_buttons")
//                .put("data", new JSONArray()
//                        .put(new JSONObject()
//                                .put("name", "🔐 Đăng nhập ngay")
//                                .put("url", "/customer/login")))
//                .toString());
//
//        // ✅ CODE ĐẦY ĐỦ (sau khi có AuthService):
//    /*
//    // Check đăng nhập
//    if (!authService.isLoggedIn(session)) {
//        return createLoginRequiredResponse();
//    }
//
//    Long userId = authService.getCurrentUserId(session);
//
//    // Extract số lượng từ message
//    int quantity = extractQuantityFromMessage(message);
//
//    // Tìm chi nhánh
//    Optional<BranchEntity> branchOpt = extractBranchFromContext(context);
//    if (branchOpt.isEmpty()) {
//        return Mono.just(new JSONObject()
//            .put("reply", "Vui lòng cho tôi biết bạn muốn đặt món tại chi nhánh nào?")
//            .toString());
//    }
//
//    BranchEntity branch = branchOpt.get();
//    String normalizedItemName = removeVietnameseTones(itemName.toLowerCase());
//
//    // Tìm món ăn
//    Optional<MenuItemEntity> itemOpt = findMenuItemByName(
//        normalizedItemName, branch.getId()
//    );
//
//    if (itemOpt.isEmpty()) {
//        return Mono.just(new JSONObject()
//            .put("reply", String.format(
//                "Không tìm thấy món '%s' tại %s.",
//                itemName, branch.getName()
//            ))
//            .toString());
//    }
//
//    MenuItemEntity menuItem = itemOpt.get();
//
//    // Thêm vào giỏ hàng
//    try {
//        AddToCartRequest request = AddToCartRequest.builder()
//            .menuItemId(menuItem.getId())
//            .quantity(quantity)
//            .isTakeaway(false)
//            .build();
//
//        cartService.addToCart(userId, request);
//
//        return Mono.just(new JSONObject()
//            .put("reply", String.format(
//                "✅ Đã thêm **%dx %s** vào giỏ hàng!\n\n" +
//                "💰 Giá: %,d₫\n\n" +
//                "Gõ 'xem giỏ hàng' để kiểm tra hoặc 'đặt món' để thanh toán.",
//                quantity, menuItem.getName(),
//                menuItem.getPrice().longValue() * quantity
//            ))
//            .put("type", "list_with_buttons")
//            .put("data", new JSONArray()
//                .put(new JSONObject()
//                    .put("name", "🛒 Xem giỏ hàng")
//                    .put("action", "view_cart"))
//                .put(new JSONObject()
//                    .put("name", "🍽️ Thêm món khác")
//                    .put("action", "view_menu")))
//            .toString());
//
//    } catch (Exception e) {
//        return Mono.just(new JSONObject()
//            .put("reply", "❌ Có lỗi khi thêm vào giỏ: " + e.getMessage())
//            .toString());
//    }
//    */
//    }


    /**
     * ===== PHẦN 1: XỬ LÝ YÊU CẦU THÊM VÀO GIỎ HÀNG =====
     */
    private Mono<String> handleAddToCart(String itemName, String message, String context) {
        System.out.println("=== ADD TO CART REQUEST ===");
        System.out.println("Item: " + itemName);

        // ✅ Parse số lượng
        int quantity = extractQuantityFromMessage(message);

        // ✅ Tìm chi nhánh từ context
        Optional<BranchEntity> branchOpt = extractBranchFromContext(context);

        if (branchOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply",
                            "Bạn muốn đặt món tại chi nhánh nào?\n\n" +
                                    "Gõ 'xem thực đơn tại [tên chi nhánh]' để xem món.")
                    .toString());
        }

        BranchEntity branch = branchOpt.get();

        // ✅ Tìm món ăn
        String normalizedItemName = removeVietnameseTones(itemName.toLowerCase().trim());
        Optional<MenuItemEntity> itemOpt = findMenuItemByName(normalizedItemName, branch.getId());

        if (itemOpt.isEmpty()) {
            return Mono.just(new JSONObject()
                    .put("reply", String.format(
                            "❌ Không tìm thấy món '%s' tại %s.\n\n" +
                                    "Gõ 'xem thực đơn' để xem tất cả món.",
                            itemName, branch.getName()))
                    .toString());
        }

        MenuItemEntity menuItem = itemOpt.get();

        // ✅ Tạo response với 2 options: Thêm giỏ hoặc Đặt ngay
        JSONObject response = new JSONObject();
        response.put("reply", String.format(
                "🍽️ **%s** - %,d₫\n\n" +
                        "Bạn muốn:\n" +
                        "• Thêm vào giỏ hàng (x%d)\n" +
                        "• Đặt món ngay (bỏ qua giỏ hàng)",
                menuItem.getName(),
                menuItem.getPrice().longValue(),
                quantity
        ));

        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();

        // Button thêm vào giỏ
        buttons.put(new JSONObject()
                .put("name", "🛒 Thêm vào giỏ hàng")
                .put("action", "add_to_cart:" + menuItem.getId() + ":" + quantity));

        // Button đặt ngay
        buttons.put(new JSONObject()
                .put("name", "⚡ Đặt món ngay")
                .put("action", "order_now:" + menuItem.getId() + ":" + quantity));

        // Button xem chi tiết
        buttons.put(new JSONObject()
                .put("name", "📋 Xem chi tiết món")
                .put("url", "/menu-items/" + menuItem.getId()));

        response.put("data", buttons);

        return Mono.just(response.toString());
    }

//    // ===== HELPER: Extract số lượng từ message =====
//    private int extractQuantityFromMessage(String message) {
//        Pattern qtyPattern = Pattern.compile("x\\s*(\\d+)");
//        Matcher matcher = qtyPattern.matcher(message.toLowerCase());
//
//        if (matcher.find()) {
//            return Integer.parseInt(matcher.group(1));
//        }
//        return 1; // Default
//    }

    /**
     * ===== HELPER: Extract số lượng từ message =====
     */
    private int extractQuantityFromMessage(String message) {
        // Pattern: "x2", "x 3", "số lượng 5"
        Pattern qtyPattern = Pattern.compile("x\\s*(\\d+)|số lượng\\s*(\\d+)");
        Matcher matcher = qtyPattern.matcher(message.toLowerCase());

        if (matcher.find()) {
            String qty = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return Integer.parseInt(qty);
        }
        return 1; // Default
    }

//    // ===== HELPER: Tìm món ăn theo tên trong chi nhánh =====
//    private Optional<MenuItemEntity> findMenuItemByName(String normalizedName, Long branchId) {
//        List<MenuCategoryEntity> categories = menuCategoryRepository
//                .findByBranchIdAndStatus(branchId, Status.ACTIVE);
//
//        for (MenuCategoryEntity category : categories) {
//            List<MenuItemEntity> items = menuItemRepository
//                    .findByCategoryIdAndStatusAndIsAvailable(
//                            category.getId(), Status.ACTIVE, true
//                    );
//
//            Optional<MenuItemEntity> found = items.stream()
//                    .filter(item -> {
//                        String itemNormalized = removeVietnameseTones(
//                                item.getName().toLowerCase()
//                        );
//                        return itemNormalized.contains(normalizedName) ||
//                                normalizedName.contains(itemNormalized);
//                    })
//                    .findFirst();
//
//            if (found.isPresent()) {
//                return found;
//            }
//        }
//
//        return Optional.empty();
//    }

    /**
            * ===== HELPER: Tìm món ăn theo tên =====
            */
    private Optional<MenuItemEntity> findMenuItemByName(String normalizedName, Long branchId) {
        List<MenuCategoryEntity> categories = menuCategoryRepository
                .findByBranchIdAndStatus(branchId, Status.ACTIVE);

        for (MenuCategoryEntity category : categories) {
            List<MenuItemEntity> items = menuItemRepository
                    .findByCategoryIdAndStatusAndIsAvailable(
                            category.getId(), Status.ACTIVE, true
                    );

            Optional<MenuItemEntity> found = items.stream()
                    .filter(item -> {
                        String itemNormalized = removeVietnameseTones(
                                item.getName().toLowerCase()
                        );
                        // Match nếu tên giống nhau hoặc chứa nhau
                        return itemNormalized.contains(normalizedName) ||
                                normalizedName.contains(itemNormalized);
                    })
                    .findFirst();

            if (found.isPresent()) {
                return found;
            }
        }

        return Optional.empty();
    }


    private String createServiceSelectionResponse(ChatbotBookingDraftEntity draft) {
        JSONObject response = new JSONObject();

        String message = String.format(
                "✅ Thông tin đã đầy đủ!\n\n" +
                        "📋 **Tóm tắt:**\n" +
                        "👤 Khách: %s\n" +
                        "📧 Email: %s\n" +
                        "📱 SĐT: %s\n\n" +
                        "Bạn có muốn thêm dịch vụ không?\n\n" +
                        "🍳 Buffet sáng: 200.000đ/người/ngày\n" +
                        "💆 Spa: 500.000đ/người\n" +
                        "🚗 Đưa đón sân bay: 300.000đ\n\n" +
                        "Trả lời 'có' để thêm dịch vụ, hoặc 'không' để bỏ qua.",
                draft.getGuestName(),
                draft.getGuestEmail(),
                draft.getGuestPhone()
        );

        response.put("reply", message);
        response.put("draftCode", draft.getDraftCode());
        response.put("type", "service_selection");

        // ✅ THÊM: Quick replies
        JSONArray quickReplies = new JSONArray();
        quickReplies.put(new JSONObject().put("text", "✅ Có, thêm dịch vụ"));
        quickReplies.put(new JSONObject().put("text", "⏭️ Không, thanh toán luôn"));
        response.put("quickReplies", quickReplies);

        // ✅ Update step
        draft.setCurrentStep(BookingDraftStep.SERVICES_SELECTING);
        draftRepository.save(draft);

        return response.toString();
    }

    private String createPaymentReadyResponse(ChatbotBookingDraftEntity draft) {
        // ✅ Tính toán giá từ BookingService
        BookingSessionDTO session = bookingService.createBookingSessionFromDraft(draft);

        draft.setTotalAmount(session.getTotalAmount());
        draft.setDepositAmount(session.getDepositAmount());
        draft.setCurrentStep(BookingDraftStep.READY_TO_PAY);
        draft = draftRepository.save(draft);

        JSONObject response = new JSONObject();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int nights = (int) ChronoUnit.DAYS.between(draft.getCheckInDate(), draft.getCheckOutDate());

        StringBuilder message = new StringBuilder();
        message.append("🎉 **Hoàn tất! Đây là tóm tắt đặt phòng:**\n\n");

        message.append("📍 **").append(draft.getBranch().getName()).append("**\n");
        message.append("   ").append(draft.getBranch().getAddress()).append("\n\n");

        message.append("🛏️ **").append(draft.getRoomType().getName()).append("**\n");
        message.append("📅 ").append(draft.getCheckInDate().format(formatter))
                .append(" → ").append(draft.getCheckOutDate().format(formatter))
                .append(" (").append(nights).append(" đêm)\n");
        message.append("👥 ").append(draft.getAdults()).append(" người lớn");
        if (draft.getChildren() > 0) {
            message.append(", ").append(draft.getChildren()).append(" trẻ em");
        }
        message.append("\n\n");

        message.append("👤 **Thông tin khách:**\n");
        message.append("   ").append(draft.getGuestName()).append("\n");
        message.append("   📧 ").append(draft.getGuestEmail()).append("\n");
        message.append("   📱 ").append(draft.getGuestPhone()).append("\n\n");

        // ✅ Hiển thị dịch vụ đã chọn
        if (Boolean.TRUE.equals(draft.getIncludeBreakfast()) ||
                Boolean.TRUE.equals(draft.getIncludeSpa()) ||
                Boolean.TRUE.equals(draft.getIncludeAirportTransfer())) {

            message.append("🎁 **Dịch vụ bổ sung:**\n");
            if (Boolean.TRUE.equals(draft.getIncludeBreakfast())) {
                message.append("   ✅ Buffet sáng\n");
            }
            if (Boolean.TRUE.equals(draft.getIncludeSpa())) {
                message.append("   ✅ Spa\n");
            }
            if (Boolean.TRUE.equals(draft.getIncludeAirportTransfer())) {
                message.append("   ✅ Đưa đón sân bay\n");
            }
            message.append("\n");
        }

        message.append("💰 **Tổng tiền:** ").append(formatCurrency(draft.getTotalAmount())).append("\n");
        message.append("💳 **Đặt cọc 50%:** ").append(formatCurrency(draft.getDepositAmount())).append("\n\n");

        message.append("👇 Nhấn nút bên dưới để tiếp tục thanh toán!");

        response.put("reply", message.toString());
        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();
        buttons.put(new JSONObject()
                .put("name", "💳 Thanh toán ngay")
                .put("url", "/bookings/from-chatbot/" + draft.getDraftCode()));

        response.put("data", buttons);

        return response.toString();
    }

    // ✅ THÊM VÀO ChatbotService.java - HANDLER GIỎ HÀNG & CHECKOUT

// ===== PHẦN 5: XEM GIỎ HÀNG =====

//    /**
//     * Xem giỏ hàng hiện tại
//     */
//    private Mono<String> handleViewCart(String context) {
//        System.out.println("=== VIEW CART ===");
//
//        // ✅ YÊU CẦU ĐĂNG NHẬP
//        return Mono.just(new JSONObject()
//                .put("reply",
//                        "🛒 **Để xem giỏ hàng, bạn cần đăng nhập.**\n\n" +
//                                "Sau khi đăng nhập, bạn có thể:\n" +
//                                "• Xem tất cả món đã chọn\n" +
//                                "• Chỉnh sửa số lượng\n" +
//                                "• Tiến hành thanh toán\n\n" +
//                                "Hoặc truy cập trực tiếp:")
//                .put("type", "list_with_buttons")
//                .put("data", new JSONArray()
//                        .put(new JSONObject()
//                                .put("name", "🔐 Đăng nhập")
//                                .put("url", "/customer/login"))
//                        .put(new JSONObject()
//                                .put("name", "🛒 Xem giỏ hàng")
//                                .put("url", "/cart")))
//                .toString());
//
//        // ✅ CODE ĐẦY ĐỦ (khi có AuthService & Session):
//    /*
//    if (!authService.isLoggedIn(session)) {
//        return createLoginRequiredResponse();
//    }
//
//    Long userId = authService.getCurrentUserId(session);
//
//    try {
//        CartSummaryResponse cart = cartService.getCartSummary(userId);
//
//        if (cart.getItems().isEmpty()) {
//            return Mono.just(new JSONObject()
//                .put("reply",
//                    "🛒 Giỏ hàng của bạn đang trống.\n\n" +
//                    "Gõ 'xem thực đơn' để bắt đầu đặt món!")
//                .toString());
//        }
//
//        StringBuilder message = new StringBuilder();
//        message.append("🛒 **Giỏ hàng của bạn:**\n\n");
//        message.append(String.format("📍 Chi nhánh: %s\n\n", cart.getBranchName()));
//
//        int index = 1;
//        for (CartItemResponse item : cart.getItems()) {
//            message.append(String.format(
//                "%d. **%s** x%d\n" +
//                "   💰 %s\n",
//                index++,
//                item.getMenuItemName(),
//                item.getQuantity(),
//                item.getFormattedSubtotal()
//            ));
//        }
//
//        message.append("\n📊 **Tổng kết:**\n");
//        message.append(String.format("• Tạm tính: %s\n", cart.getFormattedSubtotal()));
//        message.append(String.format("• Phí phục vụ: %s\n", cart.getFormattedServiceCharge()));
//        message.append(String.format("• VAT: %s\n", cart.getFormattedVat()));
//        message.append(String.format("\n💵 **Tổng: %s**\n", cart.getFormattedTotalAmount()));
//
//        JSONObject response = new JSONObject();
//        response.put("reply", message.toString());
//        response.put("type", "list_with_buttons");
//
//        JSONArray buttons = new JSONArray();
//        buttons.put(new JSONObject()
//            .put("name", "✅ Đặt món ngay")
//            .put("url", "/checkout/customer-info"));
//        buttons.put(new JSONObject()
//            .put("name", "✏️ Chỉnh sửa giỏ")
//            .put("url", "/cart"));
//        buttons.put(new JSONObject()
//            .put("name", "🍽️ Thêm món khác")
//            .put("action", "view_menu"));
//
//        response.put("data", buttons);
//        return Mono.just(response.toString());
//
//    } catch (Exception e) {
//        return Mono.just(new JSONObject()
//            .put("reply", "❌ Lỗi khi xem giỏ hàng: " + e.getMessage())
//            .toString());
//    }
//    */
//    }
    /**
     * ===== PHẦN 2: XỬ LÝ XEM GIỎ HÀNG =====
     */
    private Mono<String> handleViewCart(String context) {
        System.out.println("=== VIEW CART REQUEST ===");

        // ✅ Response yêu cầu đăng nhập + link xem giỏ
        JSONObject response = new JSONObject();
        response.put("reply",
                "🛒 **Giỏ hàng của bạn**\n\n" +
                        "Để xem giỏ hàng, vui lòng:\n" +
                        "• Nhấn nút bên dưới để mở giỏ hàng\n" +
                        "• Hoặc truy cập trực tiếp tại trang web");

        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();
        buttons.put(new JSONObject()
                .put("name", "🛒 Mở giỏ hàng")
                .put("url", "/cart"));
        buttons.put(new JSONObject()
                .put("name", "🍽️ Tiếp tục mua sắm")
                .put("action", "view_menu"));

        response.put("data", buttons);

        return Mono.just(response.toString());
    }


// ===== PHẦN 6: ĐẶT MÓN / CHECKOUT =====

//    /**
//     * Tiến hành đặt món (checkout)
//     */
//    private Mono<String> handleOrderFood(String context) {
//        System.out.println("=== ORDER FOOD ===");
//
//        // ✅ YÊU CẦU ĐĂNG NHẬP
//        return Mono.just(new JSONObject()
//                .put("reply",
//                        "🍽️ **Sẵn sàng đặt món!**\n\n" +
//                                "Để tiếp tục, bạn cần:\n" +
//                                "1️⃣ Đăng nhập hệ thống\n" +
//                                "2️⃣ Có ít nhất 1 món trong giỏ hàng\n\n" +
//                                "Tôi sẽ đưa bạn đến trang nhập thông tin và thanh toán:")
//                .put("type", "list_with_buttons")
//                .put("data", new JSONArray()
//                        .put(new JSONObject()
//                                .put("name", "🔐 Đăng nhập để đặt món")
//                                .put("url", "/customer/login?redirect=/checkout/customer-info"))
//                        .put(new JSONObject()
//                                .put("name", "🛒 Kiểm tra giỏ hàng")
//                                .put("url", "/cart")))
//                .toString());
//
//        // ✅ CODE ĐẦY ĐỦ (khi có AuthService):
//    /*
//    if (!authService.isLoggedIn(session)) {
//        return createLoginRequiredResponse();
//    }
//
//    Long userId = authService.getCurrentUserId(session);
//
//    try {
//        CartSummaryResponse cart = cartService.getCartSummary(userId);
//
//        if (cart.getItems().isEmpty()) {
//            return Mono.just(new JSONObject()
//                .put("reply",
//                    "⚠️ Giỏ hàng của bạn đang trống!\n\n" +
//                    "Vui lòng thêm món trước khi đặt hàng.")
//                .put("type", "list_with_buttons")
//                .put("data", new JSONArray()
//                    .put(new JSONObject()
//                        .put("name", "🍽️ Xem thực đơn")
//                        .put("action", "view_menu")))
//                .toString());
//        }
//
//        // Tạo summary ngắn gọn
//        StringBuilder summary = new StringBuilder();
//        summary.append("📋 **Xác nhận đặt món:**\n\n");
//        summary.append(String.format("📍 %s\n", cart.getBranchName()));
//        summary.append(String.format("🍽️ %d món\n", cart.getTotalItems()));
//        summary.append(String.format("💰 Tổng: %s\n\n", cart.getFormattedTotalAmount()));
//        summary.append("Bạn sẽ được chuyển đến trang nhập thông tin giao hàng và chọn phương thức thanh toán.");
//
//        JSONObject response = new JSONObject();
//        response.put("reply", summary.toString());
//        response.put("type", "list_with_buttons");
//
//        JSONArray buttons = new JSONArray();
//        buttons.put(new JSONObject()
//            .put("name", "✅ Tiếp tục đặt món")
//            .put("url", "/checkout/customer-info"));
//        buttons.put(new JSONObject()
//            .put("name", "✏️ Chỉnh sửa giỏ")
//            .put("url", "/cart"));
//
//        response.put("data", buttons);
//        return Mono.just(response.toString());
//
//    } catch (Exception e) {
//        return Mono.just(new JSONObject()
//            .put("reply", "❌ Lỗi khi đặt món: " + e.getMessage())
//            .toString());
//    }
//    */
//    }

    /**
     * ===== PHẦN 3: XỬ LÝ CHECKOUT =====
     */
    private Mono<String> handleOrderFood(String context) {
        System.out.println("=== CHECKOUT REQUEST ===");

        JSONObject response = new JSONObject();
        response.put("reply",
                "💳 **Sẵn sàng thanh toán!**\n\n" +
                        "Nhấn nút bên dưới để:\n" +
                        "• Xem lại giỏ hàng\n" +
                        "• Nhập thông tin giao hàng\n" +
                        "• Chọn phương thức thanh toán");

        response.put("type", "list_with_buttons");

        JSONArray buttons = new JSONArray();
        buttons.put(new JSONObject()
                .put("name", "🛒 Xem giỏ hàng")
                .put("url", "/cart"));
        buttons.put(new JSONObject()
                .put("name", "💳 Thanh toán ngay")
                .put("url", "/checkout/customer-info"));

        response.put("data", buttons);

        return Mono.just(response.toString());
    }

// ===== PHẦN 7: LOGIN REQUIRED RESPONSE =====

    /**
     * Response yêu cầu đăng nhập
     */
    private Mono<String> createLoginRequiredResponse() {
        JSONObject response = new JSONObject();
        response.put("reply",
                "🔐 **Yêu cầu đăng nhập**\n\n" +
                        "Tính năng này cần đăng nhập. Sau khi đăng nhập, bạn có thể:\n" +
                        "• Thêm món vào giỏ hàng\n" +
                        "• Xem và quản lý giỏ hàng\n" +
                        "• Đặt món và thanh toán\n" +
                        "• Theo dõi đơn hàng\n\n" +
                        "Vui lòng đăng nhập và quay lại chat!");
        response.put("type", "list_with_buttons");
        response.put("data", new JSONArray()
                .put(new JSONObject()
                        .put("name", "🔐 Đăng nhập ngay")
                        .put("url", "/customer/login"))
                .put(new JSONObject()
                        .put("name", "📝 Đăng ký tài khoản")
                        .put("url", "/customer/register")));

        return Mono.just(response.toString());
    }

    // ✅ THÊM: Helper methods
    private boolean isInfoComplete(ChatbotBookingDraftEntity draft) {
        return draft.getGuestName() != null &&
                draft.getGuestEmail() != null &&
                draft.getGuestPhone() != null;
    }

    private Optional<ChatbotBookingDraftEntity> findActiveDraftFromContext(String context) {
        // Tìm draft code trong context hoặc từ session gần nhất
        Pattern draftPattern = Pattern.compile("DRAFT\\d+");
        Matcher matcher = draftPattern.matcher(context);

        if (matcher.find()) {
            return draftRepository.findByDraftCode(matcher.group());
        }

        return Optional.empty();
    }

    private Optional<RoomTypeEntity> extractRoomTypeFromContext(String context) {
        // Sử dụng logic existing để extract room type
        return Optional.empty(); // TODO: Implement
    }

    private LocalDate[] extractDatesFromContext(String context) {
        // Sử dụng logic existing để extract dates
        return null; // TODO: Implement
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f₫", amount.doubleValue());
    }




    //
}