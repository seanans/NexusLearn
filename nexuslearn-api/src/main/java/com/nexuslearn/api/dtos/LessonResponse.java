package com.nexuslearn.api.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class LessonResponse {
    private UUID id;
    private String title;
    private String content;
    private Integer orderIndex;
    private boolean isPublished;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentResponse> attachments;
}
