package com.nexuslearn.api.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChannelMemberId implements Serializable {
    @Column(name = "channel_id")
    private UUID channelId;

    @Column(name = "user_id")
    private UUID userId;
}