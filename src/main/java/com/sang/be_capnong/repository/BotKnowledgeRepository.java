package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.BotKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotKnowledgeRepository extends JpaRepository<BotKnowledge, Integer> {
    List<BotKnowledge> findByActiveTrue();
    List<BotKnowledge> findByTypeIgnoreCaseAndActiveTrue(String type);

    @Query("SELECT k FROM BotKnowledge k WHERE k.active = true AND (" +
           "LOWER(k.title) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(k.keywords) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(k.content) LIKE LOWER(CONCAT('%', :kw, '%')))")
    List<BotKnowledge> searchByKeyword(@Param("kw") String kw);
}
