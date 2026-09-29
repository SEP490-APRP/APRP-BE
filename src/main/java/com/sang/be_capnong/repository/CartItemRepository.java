package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    void deleteByIdIn(List<Integer> ids);
}
