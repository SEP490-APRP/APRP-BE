package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.UserAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAllergyRepository extends JpaRepository<UserAllergy, Integer> {
    List<UserAllergy> findByBuyer_Id(Integer buyerId);
    boolean existsByBuyer_IdAndAllergen_Id(Integer buyerId, Integer allergenId);
    void deleteByBuyer_IdAndAllergen_Id(Integer buyerId, Integer allergenId);
    void deleteByBuyer_Id(Integer buyerId);
}
