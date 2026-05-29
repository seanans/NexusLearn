package com.nexuslearn.api.dtos;

import com.nexuslearn.api.models.EntityType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class AttachmentResponse {
    private UUID id;
    private UUID entityId;
    private EntityType entityType;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private LocalDateTime createdAt;
}