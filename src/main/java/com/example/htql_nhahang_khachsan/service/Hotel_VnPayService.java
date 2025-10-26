package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.config.Hotel_VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service xử lý thanh toán VNPay cho đặt món (Order/Checkout)
 */
@Service
public class Hotel_VnPayService {

    @Value("${vnpay.tmnCode:OS32CZUC}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret:PDPW28OZCOHDW10WKQNJ7BBVQT63Z8CM}")
    private String vnp_HashSecret;

    @Value("${vnpay.payUrl:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String vnp_PayUrl;

    @Value("${vnpay.hotel.returnUrl:/checkout/payment/vnpay-return}")
    private String vnp_ReturnUrl;

    /**
     * Tạo payment url cho đặt món
     */
    public String createPaymentUrl(long amount, String orderInfo, String orderCode, HttpServletRequest request) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", orderCode);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");

            // Build full return URL
            String finalReturnUrl = vnp_ReturnUrl;
            if (!finalReturnUrl.toLowerCase().startsWith("http")) {
                String baseUrl = request.getScheme() + "://" + request.getServerName();
                int port = request.getServerPort();
                if (port != 80 && port != 443) {
                    baseUrl += ":" + port;
                }
                if (!finalReturnUrl.startsWith("/")) {
                    finalReturnUrl = "/" + finalReturnUrl;
                }
                finalReturnUrl = baseUrl + finalReturnUrl;
            }

            System.out.println(">>> Hotel VNPay Return URL: " + finalReturnUrl);

            vnp_Params.put("vnp_ReturnUrl", finalReturnUrl);

            // IP Address
            String ipAddr = Hotel_VNPayConfig.getIpAddress(request);
            vnp_Params.put("vnp_IpAddr", ipAddr);

            // Create date
            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            // Expire date
            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            // Sort keys
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    String encodedName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.name());
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.name());


                    hashData.append(fieldName).append('=').append(encodedValue).append('&');
                    query.append(encodedName).append('=').append(encodedValue).append('&');
                }
            }

            // Remove last '&'
            if (hashData.length() > 0) hashData.deleteCharAt(hashData.length() - 1);
            if (query.length() > 0) query.deleteCharAt(query.length() - 1);

            // Compute secure hash
            String secureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
            query.append("&vnp_SecureHash=").append(secureHash);

            return vnp_PayUrl + "?" + query.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to create Hotel VNPay url: " + ex.getMessage(), ex);
        }
    }

    private String hmacSHA512(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmac512.init(secretKey);
        byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : bytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    }
}