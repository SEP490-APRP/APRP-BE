package com.sang.be_capnong.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.sang.be_capnong.entity.InvalidatedToken;

@Repository
public interface InvalidatedTokenRepository
        extends JpaRepository<InvalidatedToken, String> {
}
