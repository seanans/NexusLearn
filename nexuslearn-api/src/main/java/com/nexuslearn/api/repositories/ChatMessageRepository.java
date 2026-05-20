package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @Query("SELECT cm FROM ChatMessage cm JOIN FETCH cm.sender WHERE cm.course.id = :courseId ORDER BY cm.createdAt DESC")
    Slice<ChatMessage> findRecentMessagesByCourse(@Param("courseId") UUID courseId, Pageable pageable);
}