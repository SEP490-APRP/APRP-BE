package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    int productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    @Column(nullable = false)
    String productName;

    String unit;
    BigDecimal sellingPrice;
    Integer stockQuantity;

    @Column(columnDefinition = "TEXT")
    String description;

    String status;
    LocalDate expiryDate;

    @Column(columnDefinition = "TEXT")
    String rejectReason;

    LocalDateTime updatedAt;

    Integer discountPercent;
    LocalDateTime discountStartDate;
    LocalDateTime discountEndDate;

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProductImage> images;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    NutritionInfo nutritionInfo;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL)
    ModerationResult moderationResult;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    List<ProductAllergen> productAllergens;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    List<ProductMystery> productMysteries;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    List<ProductCombo> productCombos;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    List<Review> reviews;
}
