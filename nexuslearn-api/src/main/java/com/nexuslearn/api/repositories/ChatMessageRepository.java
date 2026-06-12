package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @Query("SELECT m FROM ChatMessage m WHERE m.channel.id = :channelId ORDER BY m.createdAt DESC")
    Slice<ChatMessage> findRecentMessagesByChannel(UUID channelId, Pageable pageable);
}