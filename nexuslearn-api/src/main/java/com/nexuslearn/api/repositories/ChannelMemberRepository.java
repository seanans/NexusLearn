package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.ChannelMember;
import com.nexuslearn.api.models.ChannelMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ChannelMemberRepository extends JpaRepository<ChannelMember, ChannelMemberId> {
    boolean existsByChannelIdAndUserId(UUID channelId, UUID userId);
}