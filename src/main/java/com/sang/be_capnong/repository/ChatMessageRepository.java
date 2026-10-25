package com.sang.be_capnong.repository;

import com.sang.be_capnong.entity.ChatMessage;
import com.sang.be_capnong.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    Page<ChatMessage> findByConversation_IdOrderBySentAtDesc(int conversationId, Pageable pageable);

    Optional<ChatMessage> findFirstByConversation_IdOrderBySentAtDesc(int conversationId);

    long countByConversation_IdAndSender_IdNotAndIsReadFalse(int conversationId, int userId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE (m.conversation.user1.id = :userId OR m.conversation.user2.id = :userId) AND m.sender.id <> :userId AND m.isRead = false")
    long countTotalUnreadForUser(@Param("userId") int userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.conversation.id = :conversationId AND m.sender.id <> :userId AND m.isRead = false")
    int markAllAsReadInConversation(@Param("conversationId") int conversationId, @Param("userId") int userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.conversation = :targetConv WHERE m.conversation.id = :oldConvId")
    int reassignMessagesToConversation(@Param("oldConvId") int oldConvId, @Param("targetConv") Conversation targetConv);
}
