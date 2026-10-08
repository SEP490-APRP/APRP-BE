package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Blog;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BlogRepository extends JpaRepository<Blog, Integer> {

    // Public: lấy bài theo status (PUBLISHED) có phân trang
    Page<Blog> findByStatus(BlogStatus status, Pageable pageable);

    // Public: lấy bài theo status và category
    Page<Blog> findByStatusAndCategory(BlogStatus status, BlogCategory category, Pageable pageable);

    // Public: lấy bài của 1 shop cụ thể
    Page<Blog> findByShop_IdAndStatus(int shopId, BlogStatus status, Pageable pageable);

    // Seller: lấy tất cả bài của shop mình (mọi trạng thái) - có phân trang
    Page<Blog> findByShop_Id(int shopId, Pageable pageable);
}
