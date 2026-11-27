package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.HubTrip;
import com.sang.be_capnong.enums.CampaignStatus;
import com.sang.be_capnong.enums.HubTripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HubTripRepository extends JpaRepository<HubTrip, Integer> {

    List<HubTrip> findByCampaign_CampaignIdAndStatusNotOrderByTripNoAsc(int campaignId, HubTripStatus status);

    List<HubTrip> findByStatusAndCampaign_StatusInOrderByPickupDateAscTripNoAsc(HubTripStatus status, Collection<CampaignStatus> campaignStatuses);

    List<HubTrip> findByDriver_IdAndStatusNotOrderByPickupDateDescTripNoAsc(int driverId, HubTripStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from HubTrip t where t.tripId = :id")
    Optional<HubTrip> findByIdForUpdate(@Param("id") int id);
}
