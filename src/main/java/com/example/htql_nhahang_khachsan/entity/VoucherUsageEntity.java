package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "voucher_usage",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "voucher_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class VoucherUsageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người dùng
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // Voucher
    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    private VoucherEntity voucher;

    // Thời gian sử dụng
    private LocalDateTime usedAt;

}
