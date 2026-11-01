package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {

    @EntityGraph(attributePaths = {"notification", "notification.admin"})
    List<UserNotification> findByUserAndIsDeletedFalseOrderByNotificationCreateAtDesc(User user);

    @EntityGraph(attributePaths = {"notification", "notification.admin"})
    Page<UserNotification> findByUserAndIsDeletedFalseOrderByNotificationCreateAtDesc(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"notification", "notification.admin"})
    Optional<UserNotification> findByIdAndUser(Integer id, User user);

    long countByUserAndIsDeletedFalseAndIsReadFalse(User user);

    List<UserNotification> findByUserAndIsDeletedFalseAndIsReadFalse(User user);

    @Modifying
    @Query("UPDATE UserNotification u SET u.isRead = true WHERE u.user = :user AND u.isDeleted = false AND u.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);
}
