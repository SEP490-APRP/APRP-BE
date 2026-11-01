package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationRequest {

    @NotBlank(message = "Notification title cannot be empty")
    @Size(max = 50, message = "Notification title cannot exceed 50 characters")
    String title;

    @NotBlank(message = "Notification message cannot be empty")
    String message;

    @Size(max = 500, message = "Evidence link cannot exceed 500 characters")
    String evidence;

    @NotEmpty(message = "Receiver types cannot be empty")
    Set<String> receiverTypes;
}
