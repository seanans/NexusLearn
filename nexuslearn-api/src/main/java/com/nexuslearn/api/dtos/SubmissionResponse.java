package com.nexuslearn.api.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class SubmissionResponse {
    private UUID id;
    private UUID assignmentId;
    private UUID userId;
    private String studentName;
    private String submissionText;
    private Integer score;
    private String feedback;
    private LocalDateTime submittedAt;
    private boolean late;
    private UUID gradedBy;
    private LocalDateTime gradedAt;
    private List<AttachmentResponse> attachments;
}