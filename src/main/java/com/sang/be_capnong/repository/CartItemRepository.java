package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    void deleteByIdIn(List<Integer> ids);

    List<CartItem> findByCart_Id(int cartId);

    Optional<CartItem> findByCart_IdAndProduct_ProductId(int cartId, int productId);

    Optional<CartItem> findByCart_IdAndMysteryBox_MysteryId(int cartId, int mysteryBoxId);

    Optional<CartItem> findByCart_IdAndBuildCombo_ComboId(int cartId, int buildComboId);
}
