package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(length = 500, nullable = false)
    String imageUrl;

    Boolean isPrimary;
    Integer displayOrder;
    LocalDateTime createdAt;

    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    Boolean isApproved = true;

    @Column(name = "is_flagged", nullable = false)
    @Builder.Default
    Boolean isFlagged = false;

    @Column(name = "flag_reason", length = 500)
    String flagReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    Product product;
}
