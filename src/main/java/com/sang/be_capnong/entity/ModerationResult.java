package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "moderation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    Boolean isFlagged;
    BigDecimal overallConfidence;
    LocalDateTime checkedAt;

    @Column(columnDefinition = "LONGTEXT")
    String rawResponse;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    Product product;

    @OneToMany(mappedBy = "moderationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ModerationLabel> moderationLabels;
}
