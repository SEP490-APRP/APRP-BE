package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer>, JpaSpecificationExecutor<Notification> {

    @Query("SELECT n FROM Notification n WHERE n.receiverType LIKE %:role% ORDER BY n.createAt DESC")
    List<Notification> findByReceiverTypeContaining(@Param("role") String role);

    @EntityGraph(attributePaths = {"admin"})
    List<Notification> findAllByOrderByCreateAtDesc();

    @EntityGraph(attributePaths = {"admin"})
    Page<Notification> findAllByOrderByCreateAtDesc(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"admin"})
    Page<Notification> findAll(Specification<Notification> spec, Pageable pageable);
}
