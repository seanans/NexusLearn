package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AssignmentUpdateRequest {
    @NotBlank(message = "Assignment title is required")
    private String title;

    private String description;

    @NotNull(message = "Max score is required")
    @Min(value = 1, message = "Max score must be at least 1")
    private Integer maxScore;

    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;

    private LocalDateTime availableFrom;

    private Boolean isPublished;

    private List<PendingAttachmentDto> newAttachments = new ArrayList<>();
}