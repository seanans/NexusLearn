package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmissionCreateRequest {
    @NotBlank(message = "Submission text cannot be empty")
    private String submissionText;
}