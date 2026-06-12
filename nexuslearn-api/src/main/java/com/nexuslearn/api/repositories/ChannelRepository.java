package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.Channel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelRepository extends JpaRepository<Channel, UUID> {
    List<Channel> findByCourseId(UUID courseId);

    Optional<Channel> findFirstByReferenceId(UUID referenceId);

    @Query(value = """
                SELECT c.id AS channelId,
                       COALESCE(
                           c.name,
                           (SELECT u.first_name || ' ' || u.last_name
                            FROM channel_members cm2
                            JOIN users u ON cm2.user_id = u.id
                            WHERE cm2.channel_id = c.id AND cm2.user_id != :userId
                            LIMIT 1),
                           'Direct Message'
                       ) AS channelName,
                       m.content,
                       COALESCE(m.created_at, c.created_at) AS message_timestamp,
                       c.type AS channelType,
                       c.course_id AS courseId,
                       cr.title AS courseTitle,
                       c.reference_id AS referenceId,
                       (SELECT a.file_name FROM attachments a WHERE a.entity_id = m.id AND a.entity_type = 'MESSAGE' LIMIT 1) as fileName,
                       CASE WHEN COALESCE(m.created_at, c.created_at) > COALESCE(my_cm.last_read_at, '1970-01-01'::timestamp) THEN true ELSE false END as hasUnread
                FROM channels c
                LEFT JOIN courses cr ON c.course_id = cr.id
                LEFT JOIN LATERAL (
                    SELECT id, content, created_at
                    FROM chat_messages cm
                    WHERE cm.channel_id = c.id
                    ORDER BY created_at DESC
                    LIMIT 1
                ) m ON true
                LEFT JOIN channel_members my_cm ON my_cm.channel_id = c.id AND my_cm.user_id = :userId
                WHERE c.id IN (SELECT channel_id FROM channel_members WHERE user_id = :userId)
                   OR (c.course_id IN (SELECT course_id FROM course_members WHERE user_id = :userId)
                       AND c.type IN ('COURSE', 'LESSON', 'ASSIGNMENT_PUBLIC', 'ANNOUNCEMENT'))
                ORDER BY COALESCE(m.created_at, c.created_at) DESC NULLS LAST
            """, nativeQuery = true)
    List<Object[]> findChatInboxDataByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT c.id FROM channels c " + "JOIN channel_members cm1 ON c.id = cm1.channel_id " + "JOIN channel_members cm2 ON c.id = cm2.channel_id " + "WHERE c.type = 'DIRECT' AND cm1.user_id = :user1 AND cm2.user_id = :user2 LIMIT 1", nativeQuery = true)
    Optional<UUID> findDirectChannelBetweenUsers(@Param("user1") UUID user1, @Param("user2") UUID user2);
}