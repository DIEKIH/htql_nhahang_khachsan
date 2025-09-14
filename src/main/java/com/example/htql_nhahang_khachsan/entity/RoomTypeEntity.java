package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "max_occupancy")
    private Integer maxOccupancy;

    @Column(name = "bed_type")
    private String bedType;

    @Column(name = "room_size")
    private Double roomSize;

    @Column(columnDefinition = "TEXT")
    private String amenities; // JSON string

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls; // JSON array

    @Enumerated(EnumType.STRING)
    private Status status; // ACTIVE, INACTIVE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
