package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @Column(length = 500, nullable = false)
    String imageUrl;

    @Enumerated(EnumType.STRING)
    MediaType mediaType;

    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "with_draw_id")
    WithdrawRequest withdrawRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id")
    ReturnRequest returnRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    Review review;
}
