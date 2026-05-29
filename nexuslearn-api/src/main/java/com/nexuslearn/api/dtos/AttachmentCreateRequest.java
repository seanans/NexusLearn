package com.nexuslearn.api.dtos;

import com.nexuslearn.api.models.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AttachmentCreateRequest {
    @NotNull
    private UUID entityId;

    @NotNull
    private EntityType entityType;

    @NotBlank
    private String fileUrl;

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileType;
}