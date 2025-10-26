package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    Optional<InvoiceEntity> findByInvoiceCode(String invoiceCode);

    Optional<InvoiceEntity> findByRoomBookingId(Long bookingId);

    List<InvoiceEntity> findByIssuedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT i FROM InvoiceEntity i WHERE i.roomBooking.branch.id = :branchId " +
            "AND i.issuedAt BETWEEN :startDate AND :endDate")
    List<InvoiceEntity> findByBranchAndDateRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
