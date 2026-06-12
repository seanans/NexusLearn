package com.nexuslearn.api.dtos;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID channelId;
    private String senderName;
    private UUID senderId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isEdited;
    private List<AttachmentResponse> attachments;
}