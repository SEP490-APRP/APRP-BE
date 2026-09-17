package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Integer> {
    Optional<OtpVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
}
