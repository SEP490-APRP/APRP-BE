package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewShopReport {
    Double averageRating;
    long totalReviewers;
    long fiveStarNumber;
    long fourStarNumber;
    long threeStarNumber;
    long twoStarNumber;
    long oneStarNumber;
}
