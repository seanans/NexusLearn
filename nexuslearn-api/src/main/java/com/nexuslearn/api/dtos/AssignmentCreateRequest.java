package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AssignmentCreateRequest {
    @NotBlank(message = "Assignment title is required")
    private String title;

    private String description;

    @NotNull(message = "Max score is required")
    @Min(value = 1, message = "Max score must be at least 1")
    private Integer maxScore;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;
}
