package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SubmissionCreateRequest {
    @NotBlank(message = "Submission text cannot be empty")
    private String submissionText;
    private List<PendingAttachmentDto> attachments = new ArrayList<>();
}