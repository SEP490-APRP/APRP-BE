package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedAddressRepository extends JpaRepository<SavedAddress, Long> {

    List<SavedAddress> findByUser_IdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(int userId);

    Optional<SavedAddress> findByIdAndUser_IdAndIsActiveTrue(Long id, int userId);

    long countByUser_IdAndIsActiveTrue(int userId);

    Optional<SavedAddress> findByUser_IdAndIsDefaultTrueAndIsActiveTrue(int userId);

    List<SavedAddress> findByUser_IdAndIsActiveTrue(int userId);
}
