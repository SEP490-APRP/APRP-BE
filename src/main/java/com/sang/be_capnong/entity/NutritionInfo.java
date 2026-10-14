package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "nutrition_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NutritionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(name = "calories")
    BigDecimal calories;

    @Column(name = "protein_g")
    BigDecimal proteinG;

    @Column(name = "carbs_g")
    BigDecimal carbsG;

    @Column(name = "fat_g")
    BigDecimal fatG;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    Product product;
}
