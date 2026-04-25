package com.nexuslearn.api.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public interface CourseDashboardProjection {
    UUID getId();
    String getTitle();
    String getDescription();
    String getTeacherFirstName();
    String getTeacherLastName();
    String getLastActivityMessage();
    LocalDateTime getLastActivityAt();
}
