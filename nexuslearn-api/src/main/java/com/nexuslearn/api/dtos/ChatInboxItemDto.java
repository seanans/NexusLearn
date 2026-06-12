package com.nexuslearn.api.dtos;

import com.nexuslearn.api.models.ChannelType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatInboxItemDto {
    private UUID channelId;
    private String channelName;
    private String latestMessageSnippet;
    private LocalDateTime latestMessageTimestamp;
    private ChannelType channelType;
    private UUID courseId;
    private String courseTitle;
    private UUID referenceId;
    private boolean hasUnread;
}