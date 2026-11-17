package com.sang.be_capnong.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_tokens", indexes = {
        @Index(name = "idx_device_token", columnList = "device_token"),
        @Index(name = "idx_user_active", columnList = "user_id, is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "device_token", length = 500, nullable = false)
    String deviceToken;

    /** ANDROID, IOS, WEB */
    @Column(length = 20)
    String platform;

    /** EXPO, FCM */
    @Column(name = "token_type", length = 30)
    String tokenType;

    @Column(name = "device_name", length = 100)
    String deviceName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "last_used_at")
    LocalDateTime lastUsedAt;

    @Column(name = "created_at")
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.lastUsedAt = now;
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.tokenType == null) {
            this.tokenType = "EXPO";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

