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
public class AssignmentResponse {
    private UUID id;
    private String title;
    private String description;
    private Integer maxScore;
    private LocalDateTime dueDate;
    private Integer orderIndex;
    private boolean published;
    private LocalDateTime availableFrom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentResponse> attachments;
}