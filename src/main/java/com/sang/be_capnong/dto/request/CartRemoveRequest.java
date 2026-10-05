package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartRemoveRequest {

    @NotEmpty(message = "Please provide at least one cart item ID to remove")
    List<Integer> cartItemIds;
}
