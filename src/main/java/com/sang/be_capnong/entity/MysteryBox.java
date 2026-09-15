package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "mystery_boxes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysteryBox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mystery_id")
    int mysteryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @Column(length = 50)
    String boxType;

    @Column(length = 500)
    String imageUrl;

    BigDecimal price;

    @Column(length = 500)
    String description;

    @Column(length = 255)
    String note;

    Boolean isActive;
    Integer soldQuantity;
    Integer totalQuantity;

    @OneToMany(mappedBy = "mysteryBox", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<ProductMystery> productMysteries;
}
