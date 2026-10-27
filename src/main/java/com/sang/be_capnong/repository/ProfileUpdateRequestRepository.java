package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ProfileUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileUpdateRequestRepository extends JpaRepository<ProfileUpdateRequest, Integer> {
    Optional<ProfileUpdateRequest> findByIdAndBuyer_Id(int id, Integer buyerId);
    List<ProfileUpdateRequest> findByBuyer_IdOrderByCreatedAtDesc(Integer buyerId);
}
