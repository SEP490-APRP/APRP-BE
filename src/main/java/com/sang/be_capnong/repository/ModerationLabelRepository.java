package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ModerationLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationLabelRepository extends JpaRepository<ModerationLabel, Integer> {
    List<ModerationLabel> findByModerationResult_Id(int moderationResultId);
}
