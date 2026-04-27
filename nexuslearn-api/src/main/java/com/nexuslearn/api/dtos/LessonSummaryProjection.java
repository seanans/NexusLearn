package com.nexuslearn.api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LessonSummaryProjection {
    UUID getId();
    String getTitle();
    String getContent();
    String getVideoUrl();
    Integer getOrderIndex();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
