package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.InvoiceDTO;
import com.example.htql_nhahang_khachsan.entity.InvoiceEntity;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceDTO getInvoiceById(Long id) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        return convertToDTO(invoice);
    }

    public InvoiceDTO getInvoiceByCode(String code) {
        InvoiceEntity invoice = invoiceRepository.findByInvoiceCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        return convertToDTO(invoice);
    }

    private InvoiceDTO convertToDTO(InvoiceEntity entity) {
        RoomBookingEntity booking = entity.getRoomBooking();

        return InvoiceDTO.builder()
                .id(entity.getId())
                .invoiceCode(entity.getInvoiceCode())
                .bookingCode(booking.getBookingCode())
                .guestName(booking.getGuestName())
                .guestPhone(booking.getGuestPhone())
                .roomNumber(booking.getRoom() != null ? booking.getRoom().getRoomNumber() : "N/A")
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfNights(booking.getNumberOfNights())
                .subtotal(entity.getSubtotal())
                .serviceFee(entity.getServiceFee())
                .vat(entity.getVat())
                .total(entity.getTotal())
                .paymentMethod(entity.getPaymentMethod())
                .issuedAt(entity.getIssuedAt())
                .issuedBy(entity.getIssuedBy())
                .build();
    }
}
