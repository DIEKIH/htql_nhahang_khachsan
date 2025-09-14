package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.ShiftStatus;
import com.example.htql_nhahang_khachsan.enums.ShiftType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(name = "customer_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPreferenceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @Column(name = "preference_type")
    private String preferenceType; // ROOM_TYPE, CUISINE, DIETARY, ACTIVITY

    @Column(name = "preference_value")
    private String preferenceValue;

    @Column(name = "preference_score")
    private Double preferenceScore; // Điểm ưu tiên (0-1)

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
