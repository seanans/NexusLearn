package com.nexuslearn.api.dtos;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID courseId;
    private String senderName;
    private UUID senderId;
    private String content;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isEdited;
}