package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewFeaturedResponse {
    Double platformAverageRating;
    long platformTotalReviews;
    List<ReviewResponse> reviews;
}
