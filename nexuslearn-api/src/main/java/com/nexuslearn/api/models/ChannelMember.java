package com.nexuslearn.api.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "channel_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChannelMember {

    @EmbeddedId
    @Builder.Default
    private ChannelMemberId id = new ChannelMemberId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("channelId")
    @JoinColumn(name = "channel_id")
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "joined_at", updatable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;
}