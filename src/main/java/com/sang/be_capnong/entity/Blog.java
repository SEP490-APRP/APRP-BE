package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "blogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    int blogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @Column(length = 255, nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(length = 500)
    String pictureUrl;

    LocalDateTime createAt;
    LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    BlogStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    BlogCategory category;

    @Builder.Default
    Integer views = 0;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    List<BlogLike> likes;

    @OneToMany(mappedBy = "blog", cascade = CascadeType.ALL, orphanRemoval = true)
    List<BlogComment> comments;
}
