package com.nexuslearn.api.dtos;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatInboxItemDto {
    private UUID courseId;
    private String courseName;
    private String latestMessageSnippet;
    private LocalDateTime latestMessageTimestamp;
}