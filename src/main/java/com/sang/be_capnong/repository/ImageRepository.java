package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Image;
import com.sang.be_capnong.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    List<Image> findByReturnRequest(ReturnRequest returnRequest);
    List<Image> findByReturnRequest_ReturnId(int returnId);
}
