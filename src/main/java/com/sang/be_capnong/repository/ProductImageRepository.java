package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(int productId);

    Optional<ProductImage> findByIdAndProduct_ProductId(Long id, int productId);

    List<ProductImage> findByProduct_ProductIdAndIsPrimaryTrue(int productId);

    long countByProduct_ProductId(int productId);
}
