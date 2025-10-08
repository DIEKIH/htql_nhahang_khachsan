package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSessionDTO implements Serializable {
    private String sessionId;
    private Long roomTypeId;
    private RoomTypeResponse roomType;

    // Booking info
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer numberOfRooms;
    private Integer adults;
    private Integer children;

    //
    // Pricing
    private BigDecimal roomPrice = BigDecimal.ZERO;
    private BigDecimal totalRoomPrice = BigDecimal.ZERO;
    private BigDecimal serviceFee = BigDecimal.ZERO;
    private BigDecimal vat = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal depositAmount = BigDecimal.ZERO;
    private BigDecimal remainingAmount = BigDecimal.ZERO;


    // Services - KHỞI TẠO GIÁ TRỊ MẶC ĐỊNH NGAY TẠI ĐÂY
    private Boolean includeBreakfast = false;
    private BigDecimal breakfastFee = BigDecimal.ZERO;
    private Boolean includeSpa = false;
    private BigDecimal spaFee = BigDecimal.ZERO;
    private Boolean includeAirportTransfer = false;
    private BigDecimal airportTransferFee = BigDecimal.ZERO;

    // Guest info
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String guestIdNumber;
    private String specialRequests;

    // XÓA CÁC GETTER THỦ CÔNG - Lombok @Data đã tạo sẵn
    // Hoặc giữ lại nhưng đơn giản hóa:

    public Boolean getIncludeBreakfast() {
        return includeBreakfast != null ? includeBreakfast : false;
    }

    public Boolean getIncludeSpa() {
        return includeSpa != null ? includeSpa : false;
    }

    public Boolean getIncludeAirportTransfer() {
        return includeAirportTransfer != null ? includeAirportTransfer : false;
    }
}
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class BookingSessionDTO {
//    private String sessionId;
//    private Long roomTypeId;
//    private RoomTypeResponse roomType;
//
//    // Booking info
//    private LocalDate checkInDate;
//    private LocalDate checkOutDate;
//    private Integer numberOfNights;
//    private Integer numberOfRooms;
//    private Integer adults;
//    private Integer children;
//
//    // Pricing
//    private BigDecimal roomPrice;
//    private BigDecimal totalRoomPrice;
//    private BigDecimal serviceFee;
//    private BigDecimal vat;
//    private BigDecimal totalAmount;
//    private BigDecimal depositAmount;
//    private BigDecimal remainingAmount;
//
//    // Services
//    private Boolean includeBreakfast = false;
//    private BigDecimal breakfastFee = BigDecimal.ZERO;
//    private Boolean includeSpa = false;
//    private BigDecimal spaFee = BigDecimal.ZERO;
//    private Boolean includeAirportTransfer = false;
//    private BigDecimal airportTransferFee = BigDecimal.ZERO;
//
//    // Guest info
//    private String guestName;
//    private String guestEmail;
//    private String guestPhone;
//    private String guestIdNumber;
//    private String specialRequests;
//
//
//
//    public RoomTypeResponse getRoomType() {
//        return roomType;
//    }
//
//    public void setRoomType(RoomTypeResponse roomType) {
//        this.roomType = roomType;
//    }
//
//    // Thêm getter để đảm bảo không trả về null
//    public Boolean getIncludeBreakfast() {
//        return includeBreakfast != null ? includeBreakfast : false;
//    }
//
//    public Boolean getIncludeSpa() {
//        return includeSpa != null ? includeSpa : false;
//    }
//
//    public Boolean getIncludeAirportTransfer() {
//        return includeAirportTransfer != null ? includeAirportTransfer : false;
//    }
//}

