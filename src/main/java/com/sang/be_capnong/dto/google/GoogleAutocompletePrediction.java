package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleAutocompletePrediction {

    private String description;

    @JsonProperty("place_id")
    private String placeId;
}
