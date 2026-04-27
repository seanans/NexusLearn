package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionGradeRequest {
    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score cannot be negative")
    private Integer score;

    private String feedback;
}