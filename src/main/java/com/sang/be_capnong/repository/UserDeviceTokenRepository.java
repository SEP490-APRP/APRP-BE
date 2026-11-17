package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.UserDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Long> {

    Optional<UserDeviceToken> findByDeviceToken(String deviceToken);

    List<UserDeviceToken> findByUserAndIsActiveTrue(User user);

    List<UserDeviceToken> findByUser_IdAndIsActiveTrue(Integer userId);

    @Query("SELECT udt FROM UserDeviceToken udt " +
           "WHERE udt.user.id IN :userIds AND udt.isActive = true")
    List<UserDeviceToken> findAllByUserIdsAndIsActiveTrue(@Param("userIds") Collection<Integer> userIds);

    @Query("SELECT udt FROM UserDeviceToken udt " +
           "JOIN udt.user u " +
           "JOIN u.role r " +
           "WHERE r.name IN :roleNames AND udt.isActive = true")
    List<UserDeviceToken> findAllByRoleNamesAndIsActiveTrue(@Param("roleNames") Collection<String> roleNames);

    @Modifying
    @Query("UPDATE UserDeviceToken udt SET udt.isActive = false, udt.updatedAt = :now WHERE udt.deviceToken = :deviceToken")
    int deactivateToken(@Param("deviceToken") String deviceToken, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserDeviceToken udt SET udt.isActive = false, udt.updatedAt = :now WHERE udt.deviceToken IN :deviceTokens")
    int deactivateTokens(@Param("deviceTokens") Collection<String> deviceTokens, @Param("now") LocalDateTime now);
}

