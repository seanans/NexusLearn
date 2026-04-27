package com.nexuslearn.api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AssignmentSummaryProjection {
    UUID getId();
    String getTitle();
    String getDescription();
    Integer getMaxScore();
    LocalDateTime getDueDate();
    Integer getOrderIndex();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
