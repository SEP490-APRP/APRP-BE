package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.MealType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "build_combos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildCombo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "combo_id")
    int comboId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @Column(length = 255, nullable = false)
    String comboName;

    BigDecimal price;

    @Column(length = 500)
    String description;

    String type;

    @Enumerated(EnumType.STRING)
    MealType mealType;

    String region;
    Integer totalQuantity;
    Integer soldQuantity;

    @Column(length = 500)
    String imageUrl;

    @Column(length = 50)
    @Builder.Default
    String status = "ACTIVE";

    @Column(length = 500)
    String cookingSuggestion;

    @OneToMany(mappedBy = "buildCombo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<ProductCombo> productCombos;
}
