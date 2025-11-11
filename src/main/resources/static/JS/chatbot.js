// File: src/main/resources/static/js/chatbot.js

(function() {
    const chatbotToggler = document.querySelector(".chatbot-toggler");
    const closeBtn = document.querySelector(".chatbot .close-btn");
    const chatbox = document.querySelector(".chatbot .chatbox");
    const chatInput = document.querySelector(".chatbot .chat-input textarea");
    const sendChatBtn = document.querySelector(".chatbot .chat-input span");

    let userMessage = null;
    const inputInitHeight = chatInput.scrollHeight;

    // ✅ Hàm tạo phần tử li cho mỗi tin nhắn
    const createChatLi = (message, className) => {
        const chatLi = document.createElement("li");
        chatLi.classList.add("chat", className);
        let chatContent = className === "outgoing"
            ? `<p></p>`
            : `<span class="material-symbols-outlined">smart_toy</span><p></p>`;
        chatLi.innerHTML = chatContent;
        chatLi.querySelector("p").textContent = message;
        return chatLi;
    };

    // ✅ Hàm sinh phản hồi từ bot
    const generateResponse = (incomingChatLi) => {
        const messageElement = incomingChatLi.querySelector("p");
        messageElement.textContent = "Đang suy nghĩ...";
        incomingChatLi.classList.add("typing");

        const requestOptions = {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                message: userMessage,
            })
        };

        // ✅ Gửi yêu cầu đến backend
        fetch("/api/chatbot", requestOptions)
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                }
                return res.json();
            })
            .then(data => {
                incomingChatLi.classList.remove("typing");

                // ✅ Kiểm tra có reply không
                if (data && data.reply) {
                    messageElement.textContent = data.reply;
                } else {
                    throw new Error("Invalid response format");
                }
            })
            .catch((error) => {
                incomingChatLi.classList.remove("typing");
                messageElement.classList.add("error");

                // ✅ Hiển thị lỗi chi tiết hơn
                console.error('❌ Chatbot error:', error);

                if (error.message.includes('404')) {
                    messageElement.textContent = "❌ Không tìm thấy API chatbot. Vui lòng kiểm tra server.";
                } else if (error.message.includes('500')) {
                    messageElement.textContent = "❌ Lỗi server. Vui lòng thử lại sau.";
                } else if (error.message.includes('Failed to fetch')) {
                    messageElement.textContent = "❌ Không thể kết nối server. Kiểm tra mạng?";
                } else {
                    messageElement.textContent = "❌ Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.";
                }
            })
            .finally(() => chatbox.scrollTo(0, chatbox.scrollHeight));
    };

    // ✅ Hàm xử lý gửi tin nhắn
    const handleChat = () => {
        userMessage = chatInput.value.trim();
        if (!userMessage) return;

        // Reset input
        chatInput.value = "";
        chatInput.style.height = `${inputInitHeight}px`;

        // Thêm tin nhắn người dùng vào chatbox
        chatbox.appendChild(createChatLi(userMessage, "outgoing"));
        chatbox.scrollTo(0, chatbox.scrollHeight);

        setTimeout(() => {
            const incomingChatLi = createChatLi("Đang suy nghĩ...", "incoming");
            chatbox.appendChild(incomingChatLi);
            chatbox.scrollTo(0, chatbox.scrollHeight);
            generateResponse(incomingChatLi);
        }, 600);
    };

    // ✅ Tự động điều chỉnh chiều cao textarea
    chatInput.addEventListener("input", () => {
        chatInput.style.height = `${inputInitHeight}px`;
        chatInput.style.height = `${chatInput.scrollHeight}px`;
    });

    // ✅ Gửi tin nhắn khi nhấn Enter
    chatInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !e.shiftKey && window.innerWidth > 800) {
            e.preventDefault();
            handleChat();
        }
    });

    // ✅ Gửi khi bấm nút gửi
    sendChatBtn.addEventListener("click", handleChat);

    // ✅ Đóng/mở chatbot
    closeBtn.addEventListener("click", () => document.body.classList.remove("show-chatbot"));
    chatbotToggler.addEventListener("click", () => document.body.classList.toggle("show-chatbot"));

    // ✅ Test connection khi load trang
    fetch("/api/chatbot/health")
        .then(res => res.text())
        .then(text => console.log("✅ Chatbot service:", text))
        .catch(err => console.error("❌ Chatbot service offline:", err));
})();