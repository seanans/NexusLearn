package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.PendingAttachmentDto;
import com.nexuslearn.api.dtos.SubmissionCreateRequest;
import com.nexuslearn.api.dtos.SubmissionGradeRequest;
import com.nexuslearn.api.dtos.SubmissionResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.repositories.AssignmentRepository;
import com.nexuslearn.api.repositories.AssignmentSubmissionRepository;
import com.nexuslearn.api.repositories.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseSecurityValidator securityValidator;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    public SubmissionResponse submitAssignment(UUID assignmentId, SubmissionCreateRequest request, User user) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        CourseRole userRole = securityValidator.getUserRoleInCourse(assignment.getModule().getCourse().getId(), user);
        if (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) {
            throw new AppException("Teachers and Assistants cannot submit assignments", HttpStatus.BAD_REQUEST);
        }

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId())
                .orElseGet(() -> AssignmentSubmission.builder().assignment(assignment).user(user).build());

        submission.setSubmissionText(request.getSubmissionText());
        submission.setScore(null);
        submission.setFeedback(null);
        submission.setGradedBy(null);
        submission.setGradedAt(null);
        submission.setSubmittedAt(LocalDateTime.now());
        submission = submissionRepository.save(submission);
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            attachmentRepository.deleteByEntityIdAndEntityType(submission.getId(), EntityType.SUBMISSION);
            for (PendingAttachmentDto stagedFile : request.getAttachments()) {
                Attachment newAttachment = new Attachment();
                newAttachment.setEntityId(submission.getId());
                newAttachment.setEntityType(EntityType.SUBMISSION);
                newAttachment.setFileUrl(stagedFile.getFileUrl());
                newAttachment.setFileName(stagedFile.getFileName());
                newAttachment.setFileType(stagedFile.getFileType());

                attachmentRepository.save(newAttachment);
            }
        }
        return mapToResponse(submission, user);
    }

    @Transactional
    public SubmissionResponse gradeSubmission(UUID submissionId, SubmissionGradeRequest request, User user) {
        AssignmentSubmission submission = submissionRepository.findByIdWithCourseContext(submissionId)
                .orElseThrow(() -> new AppException("Submission not found", HttpStatus.NOT_FOUND));

        Assignment assignment = submission.getAssignment();
        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);

        if (request.getScore() > assignment.getMaxScore()) {
            throw new AppException("Score exceeds maximum", HttpStatus.BAD_REQUEST);
        }

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setGradedBy(user);
        submission.setGradedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);
        return mapToResponse(submission, user);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsForAssignment(UUID assignmentId, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId)
                .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        CourseRole userRole = securityValidator.getUserRoleInCourse(assignment.getModule().getCourse().getId(), user);

        if (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) {
            return submissionRepository.findByAssignmentId(assignmentId).stream()
                    .map(sub -> mapToResponse(sub, user))
                    .collect(Collectors.toList());
        } else {
            SubmissionResponse singleResponse = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId())
                    .map(sub -> mapToResponse(sub, user))
                    .orElseThrow(() -> new AppException("No submission found for this user", HttpStatus.NOT_FOUND));
            return List.of(singleResponse);
        }
    }

    private SubmissionResponse mapToResponse(AssignmentSubmission submission, User user) {
        LocalDateTime actualSubmissionTime = submission.getSubmittedAt() != null ?
                submission.getSubmittedAt() : submission.getUpdatedAt();

        boolean isLate = actualSubmissionTime != null && actualSubmissionTime.isAfter(submission.getAssignment().getDueDate());

        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .userId(submission.getUser().getId())
                .studentName(submission.getUser().getFirstName() + " " + submission.getUser().getLastName())
                .submissionText(submission.getSubmissionText())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .submittedAt(actualSubmissionTime)
                .gradedAt(submission.getGradedAt())
                .late(isLate)
                .gradedBy(submission.getGradedBy() != null ? submission.getGradedBy().getId() : null)
                .attachments(attachmentService.getAttachmentsForEntity(submission.getId(), EntityType.SUBMISSION, user))
                .build();
    }
}
