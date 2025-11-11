package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.ReviewType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long roomBookingId;

    /**
     * ✅ NEW: ID của loại phòng (bắt buộc nếu type = ROOM)
     */
    private Long roomTypeId;

    /**
     * ✅ NEW: ID của món ăn (bắt buộc nếu type = RESTAURANT)
     */
    private Long menuItemId;

    @NotNull(message = "Review type is required")
    private ReviewType type;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotBlank(message = "Comment is required")
    @Size(min = 10, max = 1000, message = "Comment must be between 10 and 1000 characters")
    private String comment;
}