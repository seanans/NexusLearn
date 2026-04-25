package com.nexuslearn.api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ModuleSummaryProjection {
    UUID getId();
    String getTitle();
    String getDescription();
    Integer getOrderIndex();
    Boolean getIsPublished();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
