package com.sang.be_capnong.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SavedAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(length = 50)
    String label; // e.g., "Nhà", "Công ty", "Nhà bố mẹ"

    @Column(nullable = false, length = 100)
    String recipientName;

    @Column(nullable = false, length = 20)
    String phone;

    @Column(nullable = false, columnDefinition = "TEXT")
    String address;

    Double latitude;

    Double longitude;

    @Builder.Default
    boolean isDefault = false;

    @Builder.Default
    boolean isActive = true;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
