package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    boolean existsByCategory_CategoryId(int categoryId);

    @Override
    @EntityGraph(attributePaths = {"category", "shop", "images"})
    Optional<Product> findById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"category", "shop"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}


