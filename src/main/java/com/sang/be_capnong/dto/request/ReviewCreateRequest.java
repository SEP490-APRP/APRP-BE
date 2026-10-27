package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewCreateRequest {
    Integer orderDetailId;
    Integer mysteryBoxId;

    @NotNull(message = "Rating star is required")
    @DecimalMin(value = "1.0", message = "Rating star must be at least 1")
    @DecimalMax(value = "5.0", message = "Rating star must be at most 5")
    BigDecimal ratingStar;

    String comment;

    String evidence;
    List<String> imageUrls;
}
