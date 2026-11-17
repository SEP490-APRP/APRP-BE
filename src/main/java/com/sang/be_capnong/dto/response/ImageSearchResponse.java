package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageSearchResponse {
    String identifiedProduce;
    Double confidence;
    @JsonProperty("isAgricultural")
    boolean isAgricultural;
    List<String> detectedKeywords;
    String searchKeyword;
    List<ProductResponse> products;
    int totalMatches;
}

