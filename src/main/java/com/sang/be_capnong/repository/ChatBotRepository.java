package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ChatBot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatBotRepository extends JpaRepository<ChatBot, Integer> {
    List<ChatBot> findByUser_IdOrderByCreatedAtAsc(int userId);
    List<ChatBot> findTop10ByUser_IdOrderByCreatedAtDesc(int userId);
    void deleteByUser_Id(int userId);
}
