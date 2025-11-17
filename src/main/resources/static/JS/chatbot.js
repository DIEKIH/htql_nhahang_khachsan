// // ✅ THÊM vào đầu file chatbot.js
// (function() {
//     const chatbotToggler = document.querySelector(".chatbot-toggler");
//     const closeBtn = document.querySelector(".chatbot .close-btn");
//     const chatbox = document.querySelector(".chatbot .chatbox");
//     const chatInput = document.querySelector(".chatbot .chat-input textarea");
//     const sendChatBtn = document.querySelector(".chatbot .chat-input span");
//
//     let userMessage = null;
//     const inputInitHeight = chatInput.scrollHeight;
//
//     // ✅ THÊM: Load lịch sử chat từ localStorage khi trang load
//     const CHAT_STORAGE_KEY = 'diek_chatbot_history';
//     const MAX_STORAGE_MESSAGES = 50; // Lưu tối đa 50 tin
//
//     // Load lịch sử chat
//     const loadChatHistory = () => {
//         try {
//             const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
//             if (savedChat) {
//                 const now = Date.now();
//                 let messages = JSON.parse(savedChat);
//
//                 // Lọc các tin nhắn chưa hết hạn
//                 messages = messages.filter(msg => now - msg.timestamp <= MESSAGE_EXPIRE_MINUTES * 60 * 1000);
//
//                 // ✅ THÊM: Lọc bỏ draft đã hoàn tất (check với backend)
//                 const draftCodes = extractDraftCodes(messages);
//                 if (draftCodes.length > 0) {
//                     checkDraftStatus(draftCodes).then(validDrafts => {
//                         // Chỉ giữ messages của draft còn valid
//                         messages = messages.filter(msg => {
//                             const msgDraftCode = extractDraftCodeFromText(msg.text);
//                             return !msgDraftCode || validDrafts.includes(msgDraftCode);
//                         });
//
//                         // Update storage
//                         localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
//
//                         // Render messages
//                         renderMessages(messages);
//                     });
//                 } else {
//                     renderMessages(messages);
//                 }
//
//                 // Cập nhật lại storage
//                 localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
//
//                 messages.forEach(msg => {
//                     const chatLi = createChatLi(msg.text, msg.type);
//                     chatbox.appendChild(chatLi);
//
//                     if (msg.buttons && msg.buttons.length > 0) {
//                         const messageElement = chatLi.querySelector("p");
//                         createButtons(msg.buttons, messageElement);
//                     }
//                 });
//
//                 chatbox.scrollTo(0, chatbox.scrollHeight);
//             }
//         } catch (e) {
//             console.error('❌ Error loading chat history:', e);
//         }
//     };
//
//     // ✅ THÊM: Hàm lưu tin nhắn vào localStorage
//     const saveChatMessage = (text, type, buttons = null) => {
//         try {
//             const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
//             let messages = savedChat ? JSON.parse(savedChat) : [];
//
//             messages.push({
//                 text: text,
//                 type: type,
//                 buttons: buttons,
//                 timestamp: new Date().toISOString()
//             });
//
//             // Giới hạn số lượng tin nhắn
//             if (messages.length > MAX_STORAGE_MESSAGES) {
//                 messages = messages.slice(-MAX_STORAGE_MESSAGES);
//             }
//
//             localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
//         } catch (e) {
//             console.error('❌ Error saving chat:', e);
//         }
//     };
//
//     // ✅ THÊM: Hàm xóa lịch sử chat
//     const clearChatHistory = () => {
//         localStorage.removeItem(CHAT_STORAGE_KEY);
//         chatbox.innerHTML = `
//             <li class="chat incoming">
//                 <span class="material-symbols-outlined">smart_toy</span>
//                 <p>Xin chào! 👋<br>Tôi có thể giúp gì cho bạn hôm nay?</p>
//             </li>
//         `;
//     };
//
//     // ✅ SỬA hàm createChatLi để không thêm tin chào mặc định
//     const createChatLi = (message, className) => {
//         const chatLi = document.createElement("li");
//         chatLi.classList.add("chat", className);
//         let chatContent = className === "outgoing"
//             ? `<p></p>`
//             : `<span class="material-symbols-outlined">smart_toy</span><p></p>`;
//         chatLi.innerHTML = chatContent;
//         chatLi.querySelector("p").innerHTML = message;
//         return chatLi;
//     };
//
//     // const getChatHistory = () => {
//     //     const messages = chatbox.querySelectorAll(".chat p");
//     //     let history = [];
//     //     messages.forEach(msg => {
//     //         history.push(msg.textContent);
//     //     });
//     //     return history.slice(-20).join("\n"); // Lưu 20 tin cuối
//     // };
//
//     // ✅ SỬA: Hàm getChatHistory sử dụng trimHistory
//     const getChatHistory = () => {
//         try {
//             const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
//             if (!savedChat) return "";
//
//             let messages = JSON.parse(savedChat);
//
//             // Trim history trước khi gửi
//             return trimHistory(messages);
//
//         } catch (e) {
//             console.error('❌ Error getting chat history:', e);
//             return "";
//         }
//     };
//
//     // const createButtons = (data, messageElement) => {
//     //     const buttonContainer = document.createElement("div");
//     //     buttonContainer.classList.add("chatbot-buttons");
//     //
//     //     data.forEach(item => {
//     //         const button = document.createElement("a");
//     //         button.href = item.url;
//     //         button.textContent = item.name;
//     //         button.classList.add("chatbot-button");
//     //         buttonContainer.appendChild(button);
//     //     });
//     //
//     //     messageElement.insertAdjacentElement('afterend', buttonContainer);
//     // };
//
//     // // ✅ SỬA: Hàm createButtons để handle action
//     // const createButtons = (data, messageElement) => {
//     //     const buttonContainer = document.createElement("div");
//     //     buttonContainer.classList.add("chatbot-buttons");
//     //
//     //     data.forEach(item => {
//     //         const button = document.createElement("button"); // ✅ SỬA: button thay vì <a>
//     //
//     //         // ✅ THÊM: Check nếu có action (đặt phòng qua chat)
//     //         if (item.action && item.action.startsWith("start_booking:")) {
//     //             const draftCode = item.action.split(":")[1];
//     //             button.textContent = item.name;
//     //             button.classList.add("chatbot-button");
//     //             button.onclick = (e) => {
//     //                 e.preventDefault();
//     //                 // Gửi message tự động để bắt đầu booking
//     //                 chatInput.value = "Đặt phòng ngay - " + draftCode;
//     //                 handleChat();
//     //             };
//     //         } else if (item.url) {
//     //             // ✅ Giữ nguyên cho các button có URL
//     //             const link = document.createElement("a");
//     //             link.href = item.url;
//     //             link.textContent = item.name;
//     //             link.classList.add("chatbot-button");
//     //             buttonContainer.appendChild(link);
//     //             return;
//     //         }
//     //
//     //         buttonContainer.appendChild(button);
//     //     });
//     //
//     //     messageElement.insertAdjacentElement('afterend', buttonContainer);
//     // };
//
//     // ✅ SỬA hàm createButtons để handle các action mới
//     const createButtons = (data, messageElement) => {
//         const buttonContainer = document.createElement("div");
//         buttonContainer.classList.add("chatbot-buttons");
//
//         data.forEach(item => {
//             // ===== XỬ LÝ ACTION (không có URL) =====
//             if (item.action) {
//                 const button = document.createElement("button");
//                 button.textContent = item.name;
//                 button.classList.add("chatbot-button");
//
//                 // Handle các loại action
//                 if (item.action.startsWith("start_booking:")) {
//                     // ✅ Giữ nguyên - Đặt phòng
//                     const draftCode = item.action.split(":")[1];
//                     button.onclick = (e) => {
//                         e.preventDefault();
//                         chatInput.value = "Đặt phòng ngay - " + draftCode;
//                         handleChat();
//                     };
//                 }
//                 else if (item.action.startsWith("view_category:")) {
//                     // ✅ MỚI - Xem món theo danh mục
//                     const categoryId = item.action.split(":")[1];
//                     button.onclick = (e) => {
//                         e.preventDefault();
//                         chatInput.value = "Xem món trong danh mục " + categoryId;
//                         handleChat();
//                     };
//                 }
//                 else if (item.action === "view_menu") {
//                     // ✅ MỚI - Xem thực đơn
//                     button.onclick = (e) => {
//                         e.preventDefault();
//                         chatInput.value = "Xem thực đơn";
//                         handleChat();
//                     };
//                 }
//                 else if (item.action === "view_cart") {
//                     // ✅ MỚI - Xem giỏ hàng
//                     button.onclick = (e) => {
//                         e.preventDefault();
//                         chatInput.value = "Xem giỏ hàng";
//                         handleChat();
//                     };
//                 }
//                 else {
//                     // Default: gửi text của button
//                     button.onclick = (e) => {
//                         e.preventDefault();
//                         chatInput.value = item.name;
//                         handleChat();
//                     };
//                 }
//
//                 buttonContainer.appendChild(button);
//             }
//             // ===== XỬ LÝ URL (có link) =====
//             else if (item.url) {
//                 const link = document.createElement("a");
//                 link.href = item.url;
//                 link.textContent = item.name;
//                 link.classList.add("chatbot-button");
//                 link.target = "_blank"; // Mở tab mới để user không mất chat
//                 buttonContainer.appendChild(link);
//             }
//         });
//
//         messageElement.insertAdjacentElement('afterend', buttonContainer);
//     };
//
// // ===================================================================
//
// // ✅ THÊM: Hàm xử lý suggestion text (nếu bot gợi ý)
//     const createSuggestion = (suggestionText, messageElement) => {
//         if (!suggestionText) return;
//
//         const suggestionDiv = document.createElement("div");
//         suggestionDiv.classList.add("chatbot-suggestion");
//         suggestionDiv.innerHTML = `<small><i class="fas fa-lightbulb"></i> ${suggestionText}</small>`;
//
//         messageElement.insertAdjacentElement('afterend', suggestionDiv);
//     };
//
//     // ✅ THÊM: Hàm trim history chỉ giữ các message quan trọng
//     const trimHistory = (messages) => {
//         const MAX_HISTORY_LENGTH = 4500; // Giữ buffer 500 ký tự
//         const IMPORTANT_KEYWORDS = ['DRAFT', 'Tên:', 'Email:', 'SĐT:', 'Chi nhánh:', 'Loại phòng:'];
//
//         // Lọc các message quan trọng
//         let importantMessages = messages.filter(msg =>
//             IMPORTANT_KEYWORDS.some(keyword => msg.text.includes(keyword))
//         );
//
//         // Thêm 5 message gần nhất
//         let recentMessages = messages.slice(-5);
//
//         // Merge và loại bỏ duplicate
//         let combinedMessages = [...new Map(
//             [...importantMessages, ...recentMessages].map(m => [m.text, m])
//         ).values()];
//
//         // Sort theo timestamp
//         combinedMessages.sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
//
//         // Trim nếu vẫn quá dài
//         let history = combinedMessages.map(m => m.text).join('\n');
//
//         if (history.length > MAX_HISTORY_LENGTH) {
//             // Chỉ giữ message có DRAFT và 3 message cuối
//             let draftMessages = combinedMessages.filter(m => m.text.includes('DRAFT'));
//             let lastMessages = combinedMessages.slice(-3);
//             combinedMessages = [...draftMessages, ...lastMessages];
//             history = combinedMessages.map(m => m.text).join('\n');
//         }
//
//         return history;
//     };
//
//     // // ✅ SỬA: generateResponse sử dụng trimmed history
//     // const generateResponse = (incomingChatLi) => {
//     //     const messageElement = incomingChatLi.querySelector("p");
//     //     const history = getChatHistory(); // ✅ Đã được trim
//     //
//     //     console.log("📊 History length:", history.length); // ✅ Debug
//     //
//     //     messageElement.textContent = "Đang suy nghĩ...";
//     //     incomingChatLi.classList.add("typing");
//     //
//     //     const requestOptions = {
//     //         method: "POST",
//     //         headers: {
//     //             "Content-Type": "application/json",
//     //         },
//     //         body: JSON.stringify({
//     //             message: userMessage,
//     //             history: history
//     //         })
//     //     };
//     //
//     //     fetch("/api/chatbot", requestOptions)
//     //         .then(res => {
//     //             if (!res.ok) {
//     //                 return res.json().then(err => {
//     //                     throw new Error(err.reply || `HTTP ${res.status}: ${res.statusText}`);
//     //                 });
//     //             }
//     //             return res.json();
//     //         })
//     //         .then(data => {
//     //             incomingChatLi.classList.remove("typing");
//     //
//     //             if (data && data.reply) {
//     //                 messageElement.innerHTML = data.reply;
//     //
//     //                 // ✅ Lưu tin nhắn bot
//     //                 saveChatMessage(data.reply, 'incoming', data.data || null);
//     //
//     //                 if (data.type === 'list_with_buttons' && Array.isArray(data.data)) {
//     //                     createButtons(data.data, messageElement);
//     //                 }
//     //             } else {
//     //                 messageElement.textContent = "Lỗi: Phản hồi không hợp lệ từ chatbot.";
//     //             }
//     //         })
//     //         .catch((error) => {
//     //             incomingChatLi.classList.remove("typing");
//     //             messageElement.classList.add("error");
//     //             console.error('❌ Chatbot error:', error);
//     //             messageElement.textContent = `❌ Xin lỗi, đã có lỗi xảy ra: ${error.message}`;
//     //         })
//     //         .finally(() => chatbox.scrollTo(0, chatbox.scrollHeight));
//     // };
//
//     // ✅ SỬA hàm generateResponse để xử lý suggestion
//     const generateResponse = (incomingChatLi) => {
//         const messageElement = incomingChatLi.querySelector("p");
//         const history = getChatHistory();
//
//         messageElement.textContent = "Đang suy nghĩ...";
//         incomingChatLi.classList.add("typing");
//
//         const requestOptions = {
//             method: "POST",
//             headers: {
//                 "Content-Type": "application/json",
//             },
//             body: JSON.stringify({
//                 message: userMessage,
//                 history: history
//             })
//         };
//
//         fetch("/api/chatbot", requestOptions)
//             .then(res => {
//                 if (!res.ok) {
//                     return res.json().then(err => {
//                         throw new Error(err.reply || `HTTP ${res.status}`);
//                     });
//                 }
//                 return res.json();
//             })
//             .then(data => {
//                 incomingChatLi.classList.remove("typing");
//
//                 if (data && data.reply) {
//                     messageElement.innerHTML = data.reply;
//
//                     // ✅ Lưu tin nhắn bot
//                     saveChatMessage(data.reply, 'incoming', data.data || null);
//
//                     // ✅ Tạo buttons nếu có
//                     if (data.type === 'list_with_buttons' && Array.isArray(data.data)) {
//                         createButtons(data.data, messageElement);
//                     }
//
//                     // ✅ MỚI: Hiển thị suggestion nếu có
//                     if (data.suggestion) {
//                         createSuggestion(data.suggestion, messageElement);
//                     }
//
//                     // ✅ Xử lý quick replies (nếu có)
//                     if (data.quickReplies && Array.isArray(data.quickReplies)) {
//                         createQuickReplies(data.quickReplies, messageElement);
//                     }
//                 } else {
//                     messageElement.textContent = "Lỗi: Phản hồi không hợp lệ.";
//                 }
//             })
//             .catch((error) => {
//                 incomingChatLi.classList.remove("typing");
//                 messageElement.classList.add("error");
//                 console.error('❌ Chatbot error:', error);
//                 messageElement.textContent = `❌ Xin lỗi: ${error.message}`;
//             })
//             .finally(() => chatbox.scrollTo(0, chatbox.scrollHeight));
//     };
//
//     // ✅ SỬA hàm handleChat để lưu tin nhắn user
//     const handleChat = () => {
//         userMessage = chatInput.value.trim();
//         if (!userMessage) return;
//
//         chatInput.value = "";
//         chatInput.style.height = `${inputInitHeight}px`;
//
//         const outgoingLi = createChatLi("", "outgoing");
//         outgoingLi.querySelector('p').textContent = userMessage;
//         chatbox.appendChild(outgoingLi);
//
//         // ✅ Lưu tin nhắn user
//         saveChatMessage(userMessage, 'outgoing');
//
//         chatbox.scrollTo(0, chatbox.scrollHeight);
//
//         setTimeout(() => {
//             const incomingChatLi = createChatLi("Đang suy nghĩ...", "incoming");
//             chatbox.appendChild(incomingChatLi);
//             chatbox.scrollTo(0, chatbox.scrollHeight);
//             generateResponse(incomingChatLi);
//         }, 600);
//     };
//
//     chatInput.addEventListener("input", () => {
//         chatInput.style.height = `${inputInitHeight}px`;
//         chatInput.style.height = `${chatInput.scrollHeight}px`;
//     });
//
//     chatInput.addEventListener("keydown", (e) => {
//         if (e.key === "Enter" && !e.shiftKey && window.innerWidth > 800) {
//             e.preventDefault();
//             handleChat();
//         }
//     });
//
//     sendChatBtn.addEventListener("click", handleChat);
//     closeBtn.addEventListener("click", () => document.body.classList.remove("show-chatbot"));
//     chatbotToggler.addEventListener("click", () => document.body.classList.toggle("show-chatbot"));
//
//     // ✅ THÊM: Load lịch sử khi trang load
//     loadChatHistory();
//
//     // ✅ THÊM: Nút xóa lịch sử chat (tùy chọn - thêm vào header chatbot)
//     // Uncomment nếu muốn dùng:
//     /*
//     const clearBtn = document.createElement('button');
//     clearBtn.innerHTML = '🗑️';
//     clearBtn.className = 'clear-chat-btn';
//     clearBtn.onclick = () => {
//         if (confirm('Bạn có chắc muốn xóa lịch sử chat?')) {
//             clearChatHistory();
//         }
//     };
//     document.querySelector('.chatbot header').appendChild(clearBtn);
//     */
//
//     fetch("/api/chatbot/health")
//         .then(res => res.text())
//         .then(text => console.log("✅ Chatbot service:", text))
//         .catch(err => console.error("❌ Chatbot service offline:", err));
//
//
//     // ✅ THÊM vào chatbot.js
//
// // Trong hàm generateResponse, sau khi nhận response:
//     if (data.quickReplies && Array.isArray(data.quickReplies)) {
//         createQuickReplies(data.quickReplies, messageElement);
//     }
//
// // ✅ THÊM hàm mới
//     const createQuickReplies = (quickReplies, messageElement) => {
//         const quickReplyContainer = document.createElement("div");
//         quickReplyContainer.classList.add("quick-replies");
//
//         quickReplies.forEach(reply => {
//             const button = document.createElement("button");
//             button.textContent = reply.text;
//             button.classList.add("quick-reply-btn");
//             button.onclick = () => {
//                 chatInput.value = reply.text;
//                 handleChat();
//             };
//             quickReplyContainer.appendChild(button);
//         });
//
//         messageElement.insertAdjacentElement('afterend', quickReplyContainer);
//     };
//
//
//     // // ✅ THÊM: Hàm cleanup messages cũ khi quá nhiều
//     // const cleanupOldMessages = () => {
//     //     try {
//     //         const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
//     //         if (!savedChat) return;
//     //
//     //         let messages = JSON.parse(savedChat);
//     //         const now = Date.now();
//     //         const ONE_HOUR = 60 * 60 * 1000;
//     //
//     //         // Xóa message cũ hơn 1 giờ (trừ message có DRAFT)
//     //         messages = messages.filter(msg => {
//     //             const age = now - new Date(msg.timestamp).getTime();
//     //             return age < ONE_HOUR || msg.text.includes('DRAFT');
//     //         });
//     //
//     //         // Giới hạn tối đa 30 message
//     //         if (messages.length > 30) {
//     //             messages = messages.slice(-30);
//     //         }
//     //
//     //         localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
//     //
//     //     } catch (e) {
//     //         console.error('❌ Cleanup error:', e);
//     //     }
//     // };
//     // ✅ SỬA: cleanupOldMessages - Aggressive cleanup
//     const cleanupOldMessages = () => {
//         try {
//             const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
//             if (!savedChat) return;
//
//             let messages = JSON.parse(savedChat);
//             const now = Date.now();
//             const ONE_HOUR = 60 * 60 * 1000;
//
//             // ✅ SỬA: Xóa message cũ hơn 1 giờ (bao gồm cả DRAFT)
//             messages = messages.filter(msg => {
//                 const age = now - new Date(msg.timestamp).getTime();
//                 return age < ONE_HOUR;
//             });
//
//             // Giới hạn tối đa 20 message (giảm từ 30)
//             if (messages.length > 20) {
//                 messages = messages.slice(-20);
//             }
//
//             localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
//
//         } catch (e) {
//             console.error('❌ Cleanup error:', e);
//         }
//     };
//
//     // ✅ THÊM: Hàm clear draft code khi user hoàn tất thanh toán
//     // Gọi từ trang confirmation/success
//     window.clearChatbotDraft = (draftCode) => {
//         removeDraftFromHistory(draftCode);
//
//         // Reload chat để loại bỏ messages
//         chatbox.innerHTML = `
//             <li class="chat incoming">
//                 <span class="material-symbols-outlined">smart_toy</span>
//                 <p>✅ Đặt phòng thành công! Cảm ơn bạn đã sử dụng dịch vụ.<br>
//                 Tôi có thể giúp gì thêm cho bạn?</p>
//             </li>
//         `;
//     };
//
//
//     // ✅ THÊM: Cleanup mỗi 5 phút
//     setInterval(cleanupOldMessages, 5 * 60 * 1000);
//
// // ✅ Gọi cleanup mỗi khi load trang
//     loadChatHistory();
//     cleanupOldMessages();
//
//
//
//     // ✅ THÊM: Hàm xóa draft code cụ thể khỏi history
//     const removeDraftFromHistory = (draftCode) => {
//         try {
//             const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
//             if (!savedChat) return;
//
//             let messages = JSON.parse(savedChat);
//
//             // Lọc bỏ messages có chứa draft code đó
//             messages = messages.filter(msg => !msg.text.includes(draftCode));
//
//             localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
//             log.info('🗑️ Removed draft {} from chat history', draftCode);
//         } catch (e) {
//             console.error('❌ Error removing draft from history:', e);
//         }
//     };
//
//
//     // ✅ THÊM: Extract draft codes từ messages
//     const extractDraftCodes = (messages) => {
//         const draftPattern = /DRAFT\d+/g;
//         const codes = new Set();
//
//         messages.forEach(msg => {
//             const matches = msg.text.match(draftPattern);
//             if (matches) {
//                 matches.forEach(code => codes.add(code));
//             }
//         });
//
//         return Array.from(codes);
//     };
//
//     // ✅ THÊM: Extract single draft code từ text
//     const extractDraftCodeFromText = (text) => {
//         const match = text.match(/DRAFT\d+/);
//         return match ? match[0] : null;
//     };
//
//     // ✅ THÊM: Check draft status với backend
//     const checkDraftStatus = async (draftCodes) => {
//         try {
//             // Call API để check draft nào còn valid
//             const response = await fetch('/api/chatbot/check-drafts', {
//                 method: 'POST',
//                 headers: { 'Content-Type': 'application/json' },
//                 body: JSON.stringify({ draftCodes })
//             });
//
//             if (response.ok) {
//                 const result = await response.json();
//                 return result.validDrafts || [];
//             }
//         } catch (e) {
//             console.error('❌ Error checking draft status:', e);
//         }
//         return draftCodes; // Fallback: giữ tất cả nếu lỗi
//     };
//
//     // ✅ THÊM: Render messages
//     const renderMessages = (messages) => {
//         messages.forEach(msg => {
//             const chatLi = createChatLi(msg.text, msg.type);
//             chatbox.appendChild(chatLi);
//
//             if (msg.buttons && msg.buttons.length > 0) {
//                 const messageElement = chatLi.querySelector("p");
//                 createButtons(msg.buttons, messageElement);
//             }
//         });
//
//         chatbox.scrollTo(0, chatbox.scrollHeight);
//     };
// })();

// ✅ CHATBOT.JS - FIXED VERSION
// ✅ CHATBOT.JS - COMPLETE FIXED VERSION
(function() {
    const chatbotToggler = document.querySelector(".chatbot-toggler");
    const closeBtn = document.querySelector(".chatbot .close-btn");
    const chatbox = document.querySelector(".chatbot .chatbox");
    const chatInput = document.querySelector(".chatbot .chat-input textarea");
    const sendChatBtn = document.querySelector(".chatbot .chat-input span");

    let userMessage = null;
    const inputInitHeight = chatInput.scrollHeight;

    const CHAT_STORAGE_KEY = 'diek_chatbot_history';
    const MAX_STORAGE_MESSAGES = 50;
    const MESSAGE_EXPIRE_MINUTES = 30;

    // ===== LOAD CHAT HISTORY =====
    const loadChatHistory = () => {
        try {
            const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
            if (!savedChat) {
                showWelcomeMessage();
                return;
            }

            const now = Date.now();
            let messages = JSON.parse(savedChat);

            messages = messages.filter(msg => {
                if (!msg.timestamp) return false;
                const messageTime = new Date(msg.timestamp).getTime();
                const ageInMinutes = (now - messageTime) / (1000 * 60);
                return ageInMinutes <= MESSAGE_EXPIRE_MINUTES;
            });

            if (messages.length === 0) {
                localStorage.removeItem(CHAT_STORAGE_KEY);
                showWelcomeMessage();
                return;
            }

            messages.forEach(msg => {
                const chatLi = createChatLi(msg.text, msg.type);
                chatbox.appendChild(chatLi);

                if (msg.buttons && msg.buttons.length > 0) {
                    const messageElement = chatLi.querySelector("p");
                    createButtons(msg.buttons, messageElement);
                }
            });

            chatbox.scrollTo(0, chatbox.scrollHeight);
            localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));

        } catch (e) {
            console.error('❌ Error loading chat history:', e);
            localStorage.removeItem(CHAT_STORAGE_KEY);
            showWelcomeMessage();
        }
    };

    const showWelcomeMessage = () => {
        chatbox.innerHTML = `
            <li class="chat incoming">
                <span class="material-symbols-outlined">smart_toy</span>
                <p>Xin chào! 👋<br>Tôi có thể giúp gì cho bạn hôm nay?</p>
            </li>
        `;
    };

    const saveChatMessage = (text, type, buttons = null, draftCode = null) => {
        try {
            console.log('=== SAVING MESSAGE ===');
            console.log('Text:', text.substring(0, 100));
            console.log('Draft code:', draftCode); // ← CHECK DÒNG NÀY

            const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
            let messages = savedChat ? JSON.parse(savedChat) : [];

            let fullText = text;
            if (draftCode) {
                fullText += `\n\n[DRAFT: ${draftCode}]`;
                console.log('✅ Added draft to text'); // ← CHECK DÒNG NÀY
            }

            messages.push({
                text: fullText,
                type: type,
                buttons: buttons,
                draftCode: draftCode,
                timestamp: new Date().toISOString()
            });

            localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
            console.log('✅ Saved to localStorage');
        } catch (e) {
            console.error('❌ Error saving chat:', e);
        }
    };

    const getChatHistory = () => {
        try {
            const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
            if (!savedChat) return "";

            let messages = JSON.parse(savedChat);
            const now = Date.now();

            messages = messages.filter(msg => {
                const messageTime = new Date(msg.timestamp).getTime();
                const ageInMinutes = (now - messageTime) / (1000 * 60);
                return ageInMinutes <= MESSAGE_EXPIRE_MINUTES;
            });

            const recentMessages = messages.slice(-10);
            return recentMessages.map(m => m.text).join('\n');

        } catch (e) {
            console.error('❌ Error getting chat history:', e);
            return "";
        }
    };

    const createChatLi = (message, className) => {
        const chatLi = document.createElement("li");
        chatLi.classList.add("chat", className);
        let chatContent = className === "outgoing"
            ? `<p></p>`
            : `<span class="material-symbols-outlined">smart_toy</span><p></p>`;
        chatLi.innerHTML = chatContent;
        chatLi.querySelector("p").innerHTML = message;
        return chatLi;
    };

    // ===== CREATE BUTTONS WITH ACTION HANDLERS =====
    const createButtons = (data, messageElement) => {
        const buttonContainer = document.createElement("div");
        buttonContainer.classList.add("chatbot-buttons");

        data.forEach(item => {
            if (item.action) {
                const button = document.createElement("button");
                button.textContent = item.name;
                button.classList.add("chatbot-button");

                // ✅ XỬ LÝ CÁC ACTION
                if (item.action.startsWith("start_booking:")) {
                    const draftCode = item.action.split(":")[1];
                    button.onclick = (e) => {
                        e.preventDefault();
                        chatInput.value = "Đặt phòng ngay - " + draftCode;
                        handleChat();
                    };
                }
                else if (item.action.startsWith("add_to_cart:")) {
                    // ✅ THÊM VÀO GIỎ HÀNG
                    const parts = item.action.split(":");
                    const menuItemId = parts[1];
                    const quantity = parts[2] || 1;
                    button.onclick = (e) => {
                        e.preventDefault();
                        addToCartFromChat(menuItemId, quantity);
                    };
                }
                else if (item.action.startsWith("order_now:")) {
                    // ✅ ĐẶT MÓN NHANH
                    const parts = item.action.split(":");
                    const menuItemId = parts[1];
                    const quantity = parts[2] || 1;
                    button.onclick = (e) => {
                        e.preventDefault();
                        startQuickOrderFromChat(menuItemId, quantity);
                    };
                }
                else if (item.action === "view_cart") {
                    button.onclick = (e) => {
                        e.preventDefault();
                        window.location.href = '/cart';
                    };
                }
                else if (item.action === "view_menu") {
                    button.onclick = (e) => {
                        e.preventDefault();
                        chatInput.value = "xem thực đơn";
                        handleChat();
                    };
                }
                else {
                    button.onclick = (e) => {
                        e.preventDefault();
                        chatInput.value = item.name;
                        handleChat();
                    };
                }

                buttonContainer.appendChild(button);
            }
            else if (item.url) {
                const link = document.createElement("a");
                link.href = item.url;
                link.textContent = item.name;
                link.classList.add("chatbot-button");
                link.target = "_blank";
                buttonContainer.appendChild(link);
            }
        });

        messageElement.insertAdjacentElement('afterend', buttonContainer);
    };

    // ===== ✅ THÊM VÀO GIỎ HÀNG - CHECK LOGIN =====
    const addToCartFromChat = async (menuItemId, quantity) => {
        try {
            console.log('🛒 Adding to cart:', menuItemId, 'x', quantity);

            // BƯỚC 1: Check đăng nhập
            const authResponse = await fetch('/api/auth/check');
            const authData = await authResponse.json();

            if (!authData.isLoggedIn) {
                const confirmLogin = confirm(
                    'Bạn cần đăng nhập để thêm món vào giỏ hàng.\n\n' +
                    'Chuyển đến trang đăng nhập?'
                );

                if (confirmLogin) {
                    sessionStorage.setItem('redirectAfterLogin', window.location.pathname);
                    window.location.href = '/customer/login';
                }
                return;
            }

            // BƯỚC 2: Gọi API thêm vào giỏ
            const response = await fetch('/api/cart/quick-add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    menuItemId: menuItemId,
                    quantity: quantity || 1
                })
            });

            const data = await response.json();

            if (data.success) {
                // Update cart counter
                if (data.cartCount) {
                    updateCartCount(data.cartCount);
                }

                // Gửi message xác nhận
                const outgoingLi = createChatLi("Đã thêm vào giỏ hàng", "outgoing");
                chatbox.appendChild(outgoingLi);
                saveChatMessage("Đã thêm vào giỏ hàng", "outgoing");

                setTimeout(() => {
                    const incomingLi = createChatLi(
                        `✅ Đã thêm món vào giỏ hàng!\n\n` +
                        `🛒 Giỏ hàng: ${data.cartCount} món\n\n` +
                        `Bạn muốn:\n` +
                        `• Tiếp tục mua sắm\n` +
                        `• Xem giỏ hàng\n` +
                        `• Thanh toán ngay`,
                        "incoming"
                    );
                    chatbox.appendChild(incomingLi);

                    const messageElement = incomingLi.querySelector("p");
                    createButtons([
                        { name: "🍽️ Thêm món khác", action: "view_menu" },
                        { name: "🛒 Xem giỏ hàng", url: "/cart" },
                        { name: "💳 Thanh toán", url: "/checkout/customer-info" }
                    ], messageElement);

                    saveChatMessage(incomingLi.querySelector("p").innerHTML, "incoming");
                    chatbox.scrollTo(0, chatbox.scrollHeight);
                }, 300);

            } else {
                throw new Error(data.message || 'Có lỗi xảy ra');
            }
        } catch (error) {
            console.error('❌ Add to cart error:', error);
            alert('❌ ' + error.message);
        }
    };

    // ===== ✅ ĐẶT MÓN NHANH - CHECK LOGIN =====
    const startQuickOrderFromChat = async (menuItemId, quantity) => {
        try {
            console.log('⚡ Quick order:', menuItemId, 'x', quantity);

            // Check đăng nhập
            const authResponse = await fetch('/api/auth/check');
            const authData = await authResponse.json();

            if (!authData.isLoggedIn) {
                const confirmLogin = confirm(
                    'Bạn cần đăng nhập để đặt món.\n\n' +
                    'Chuyển đến trang đăng nhập?'
                );

                if (confirmLogin) {
                    sessionStorage.setItem('redirectAfterLogin', window.location.pathname);
                    sessionStorage.setItem('pendingQuickOrder', JSON.stringify({
                        menuItemId: menuItemId,
                        quantity: quantity
                    }));
                    window.location.href = '/customer/login';
                }
                return;
            }

            // Gửi message xác nhận
            const outgoingLi = createChatLi(`Đặt món ngay (x${quantity})`, "outgoing");
            chatbox.appendChild(outgoingLi);
            saveChatMessage(`Đặt món ngay (x${quantity})`, "outgoing");

            setTimeout(() => {
                const incomingLi = createChatLi(
                    `📋 **Xác nhận đặt món nhanh**\n\n` +
                    `Số lượng: x${quantity}\n\n` +
                    `Tôi sẽ thu thập thông tin để hoàn tất đơn hàng.\n\n` +
                    `Bạn có thể gửi:\n` +
                    `\`\`\`\n` +
                    `Tên: Nguyễn Văn A\n` +
                    `SĐT: 0912345678\n` +
                    `Địa chỉ: 123 Lê Lợi, Q1\n` +
                    `\`\`\`\n\n` +
                    `Hoặc từng thông tin riêng lẻ.`,
                    "incoming"
                );
                chatbox.appendChild(incomingLi);

                // Lưu pending order
                sessionStorage.setItem('pendingQuickOrder', JSON.stringify({
                    menuItemId: menuItemId,
                    quantity: quantity,
                    step: 'collecting_info'
                }));

                saveChatMessage(incomingLi.querySelector("p").innerHTML, "incoming");
                chatbox.scrollTo(0, chatbox.scrollHeight);
            }, 300);

        } catch (error) {
            console.error('❌ Quick order error:', error);
            alert('❌ ' + error.message);
        }
    };

    // ===== ✅ UPDATE CART COUNT =====
    const updateCartCount = (count) => {
        const cartBadge = document.querySelector('.cart-count');
        if (cartBadge) {
            cartBadge.textContent = count;
            if (count > 0) {
                cartBadge.style.display = 'inline-block';
            }
        }
    };

    const generateResponse = (incomingChatLi) => {
        const messageElement = incomingChatLi.querySelector("p");
        const history = getChatHistory();

        messageElement.textContent = "Đang suy nghĩ...";
        incomingChatLi.classList.add("typing");

        const requestOptions = {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                message: userMessage,
                history: history
            })
        };

        fetch("/api/chatbot", requestOptions)
            .then(res => {
                if (!res.ok) {
                    return res.json().then(err => {
                        throw new Error(err.reply || `HTTP ${res.status}`);
                    });
                }
                return res.json();
            })
            .then(data => {
                incomingChatLi.classList.remove("typing");

                if (data && data.reply) {
                    messageElement.innerHTML = data.reply;

                    // ✅ SỬA: Lưu tin nhắn bot với draft code
                    saveChatMessage(
                        data.reply,
                        'incoming',
                        data.data || null,
                        data.draftCode || null // ← THÊM PARAM NÀY
                    );

                    // Tạo buttons nếu có
                    if (data.type === 'list_with_buttons' && Array.isArray(data.data)) {
                        createButtons(data.data, messageElement);
                    }

                    if (data.quickReplies && Array.isArray(data.quickReplies)) {
                        createQuickReplies(data.quickReplies, messageElement);
                    }
                } else {
                    messageElement.textContent = "Lỗi: Phản hồi không hợp lệ.";
                }
            })
            .catch((error) => {
                incomingChatLi.classList.remove("typing");
                messageElement.classList.add("error");
                console.error('❌ Chatbot error:', error);
                messageElement.textContent = `❌ Xin lỗi: ${error.message}`;
            })
            .finally(() => chatbox.scrollTo(0, chatbox.scrollHeight));
    };

    const createQuickReplies = (quickReplies, messageElement) => {
        const quickReplyContainer = document.createElement("div");
        quickReplyContainer.classList.add("quick-replies");

        quickReplies.forEach(reply => {
            const button = document.createElement("button");
            button.textContent = reply.text;
            button.classList.add("quick-reply-btn");
            button.onclick = () => {
                chatInput.value = reply.text;
                handleChat();
            };
            quickReplyContainer.appendChild(button);
        });

        messageElement.insertAdjacentElement('afterend', quickReplyContainer);
    };

    const handleChat = () => {
        userMessage = chatInput.value.trim();
        if (!userMessage) return;

        chatInput.value = "";
        chatInput.style.height = `${inputInitHeight}px`;

        const outgoingLi = createChatLi("", "outgoing");
        outgoingLi.querySelector('p').textContent = userMessage;
        chatbox.appendChild(outgoingLi);
        saveChatMessage(userMessage, 'outgoing');

        chatbox.scrollTo(0, chatbox.scrollHeight);

        setTimeout(() => {
            const incomingChatLi = createChatLi("Đang suy nghĩ...", "incoming");
            chatbox.appendChild(incomingChatLi);
            chatbox.scrollTo(0, chatbox.scrollHeight);
            generateResponse(incomingChatLi);
        }, 600);
    };

    const cleanupExpiredMessages = () => {
        try {
            const savedChat = localStorage.getItem(CHAT_STORAGE_KEY);
            if (!savedChat) return;

            const now = Date.now();
            let messages = JSON.parse(savedChat);

            messages = messages.filter(msg => {
                const messageTime = new Date(msg.timestamp).getTime();
                const ageInMinutes = (now - messageTime) / (1000 * 60);
                return ageInMinutes <= MESSAGE_EXPIRE_MINUTES;
            });

            if (messages.length === 0) {
                localStorage.removeItem(CHAT_STORAGE_KEY);
            } else {
                localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(messages));
            }
        } catch (e) {
            console.error('❌ Cleanup error:', e);
        }
    };

    // Event listeners
    chatInput.addEventListener("input", () => {
        chatInput.style.height = `${inputInitHeight}px`;
        chatInput.style.height = `${chatInput.scrollHeight}px`;
    });

    chatInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey && window.innerWidth > 800) {
            e.preventDefault();
            handleChat();
        }
    });

    sendChatBtn.addEventListener("click", handleChat);
    closeBtn.addEventListener("click", () => document.body.classList.remove("show-chatbot"));
    chatbotToggler.addEventListener("click", () => document.body.classList.toggle("show-chatbot"));

    // Initialize
    loadChatHistory();
    setInterval(cleanupExpiredMessages, 5 * 60 * 1000);

    // Health check
    fetch("/api/chatbot/health")
        .then(res => res.text())
        .then(text => console.log("✅ Chatbot service:", text))
        .catch(err => console.error("❌ Chatbot service offline:", err));
})();