package com.sang.be_capnong.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIdNot(String phoneNumber, int id);
    boolean existsByCccd(String cccd);

    List<User> findByRole_NameInAndStatus(Collection<String> roleNames, UserStatus status);
    List<User> findByRole_NameIn(Collection<String> roleNames);
    Page<User> findByRole_NameInAndStatus(Collection<String> roleNames, UserStatus status, Pageable pageable);
    Page<User> findByRole_NameIn(Collection<String> roleNames, Pageable pageable);
    Page<User> findByStatus(UserStatus status, Pageable pageable);
    long countByRole_NameInAndStatus(Collection<String> roleNames, UserStatus status);
    /** Shop có toạ độ nằm trong khung (lọc thô trước khi tính Haversine) */
    @Query("select u from User u where u.role.name = 'SHOP' and u.status = com.sang.be_capnong.enums.UserStatus.ACTIVE "
            + "and u.shopLatitude between :minLat and :maxLat and u.shopLongitude between :minLng and :maxLng")
    List<User> findShopsInBox(@Param("minLat") double minLat, @Param("maxLat") double maxLat,
                              @Param("minLng") double minLng, @Param("maxLng") double maxLng);

    List<User> findByStatusAndLockedAtBefore(UserStatus status, java.time.LocalDateTime time);
}
