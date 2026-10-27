package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Allergen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AllergenRepository extends JpaRepository<Allergen, Integer> {
    Optional<Allergen> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
