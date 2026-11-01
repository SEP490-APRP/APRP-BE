package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.AiAccountActionType;
import com.sang.be_capnong.enums.AiIntentType;
import com.sang.be_capnong.enums.AiItemType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDetectedIntent {
    AiIntentType intent;
    String keyword;
    AiItemType itemType;
    Integer quantity;
    Integer selectedIndex;
    AiAccountActionType accountAction;
    Integer orderId;
    BigDecimal budget;
    String ingredientsQuery;
    List<ItemBuyCommand> buyItems;
    String replyText;
    Integer month;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemBuyCommand {
        String name;
        Double quantity;
        String unit;
    }
}
