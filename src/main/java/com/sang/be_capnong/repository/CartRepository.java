package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Cart;
import com.sang.be_capnong.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser(User user);
}
